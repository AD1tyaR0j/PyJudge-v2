package com.pyjudge.judge;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The part every sandbox shares: a throwaway directory holding the program and its input,
 * a child process with redirected streams, a wall-clock timeout, and output size caps.
 * Subclasses decide which command runs the program and how to tell the failures apart.
 */
public abstract class AbstractSandbox implements Sandbox {
    private static final String PROGRAM = "main.py";
    private static final String CHECKER = "check_syntax.py";
    private static final int CHECK_TIME_LIMIT = 10;
    private static final int CHECK_MEMORY_LIMIT = 256;
    /** Compiles main.py without running it and prints the error the way the interpreter would. */
    private static final String CHECKER_SOURCE = """
            import sys, traceback
            try:
                with open("main.py", encoding="utf-8", newline="") as f:
                    source = f.read()
                compile(source, "main.py", "exec")
            except SyntaxError as e:
                sys.stdout.write("".join(traceback.format_exception_only(type(e), e)).strip())
                sys.exit(1)
            except Exception as e:  # e.g. ValueError for null bytes, RecursionError for deep nesting
                sys.stdout.write(f"{type(e).__name__}: {e}")
                sys.exit(1)
            """;

    /** The command that runs {@code script} (a file inside {@code dir}). */
    protected abstract List<String> command(Path dir, String script, int timeLimitSeconds, int memoryLimitMb,
                                            String runId);

    /** Environment variables for the child process. */
    protected abstract Map<String, String> environment();

    /** Seconds to wait before declaring a time-limit violation. */
    protected int waitSeconds(int timeLimitSeconds) {
        return timeLimitSeconds;
    }

    /** Called after a timeout, before the process is killed. */
    protected void onTimeout(String runId) {
    }

    protected abstract RunStatus classify(boolean timedOut, int exitCode, String stderr);

    @Override
    public RunResult run(String code, String stdin, int timeLimitSeconds, int memoryLimitMb, int maxOutputBytes) {
        return inTempDir(dir -> {
            write(dir.resolve(PROGRAM), code);
            return execute(dir, PROGRAM, stdin, timeLimitSeconds, memoryLimitMb, maxOutputBytes);
        });
    }

    @Override
    public String checkSyntax(String code) {
        return inTempDir(dir -> {
            write(dir.resolve(PROGRAM), code);
            write(dir.resolve(CHECKER), CHECKER_SOURCE);
            RunResult result = execute(dir, CHECKER, "", CHECK_TIME_LIMIT, CHECK_MEMORY_LIMIT, 16 * 1024);
            if (result.exitCode() != null && result.exitCode() == 1 && !result.stdout().isBlank()) {
                return result.stdout().strip();
            }
            return null;
        });
    }

    private RunResult execute(Path dir, String script, String stdin, int timeLimitSeconds, int memoryLimitMb,
                              int maxOutputBytes) throws IOException {
        Path in = dir.resolve("input.txt");
        Path out = dir.resolve("stdout.txt");
        Path err = dir.resolve("stderr.txt");
        write(in, stdin == null ? "" : stdin);

        String runId = "judge_" + Long.toHexString(System.nanoTime()) + Long.toHexString(Thread.currentThread().getId());
        ProcessBuilder builder = new ProcessBuilder(command(dir, script, timeLimitSeconds, memoryLimitMb, runId))
                .directory(dir.toFile())
                .redirectInput(in.toFile())
                .redirectOutput(out.toFile())
                .redirectError(err.toFile());
        builder.environment().clear();
        builder.environment().putAll(environment());

        long start = System.nanoTime();
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new SandboxException("Could not start " + builder.command().get(0)
                    + ". Install Python 3 (or Docker) on the server, or set python.executable.", e);
        }
        boolean timedOut = false;
        int exitCode;
        try {
            if (process.waitFor(waitSeconds(timeLimitSeconds), TimeUnit.SECONDS)) {
                exitCode = process.exitValue();
            } else {
                timedOut = true;
                onTimeout(runId);
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                exitCode = process.waitFor();
            }
        } catch (InterruptedException e) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new SandboxException("Interrupted while running code.", e);
        }
        int elapsedMs = (int) ((System.nanoTime() - start) / 1_000_000);

        String stdout = read(out, maxOutputBytes);
        // Hide the temp directory from tracebacks
        String stderr = read(err, maxOutputBytes)
                .replace(dir + java.io.File.separator, "")
                .replace(dir.toString(), "");
        RunStatus status = classify(timedOut, exitCode, stderr);
        return new RunResult(status, stdout, stderr, timedOut ? null : exitCode, elapsedMs);
    }

    // ------------------------------------------------------------ files

    @FunctionalInterface
    private interface DirWork<T> {
        T run(Path dir) throws IOException;
    }

    private static <T> T inTempDir(DirWork<T> work) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("judge_").toRealPath();
            return work.run(dir);
        } catch (IOException e) {
            throw new SandboxException("The sandbox could not use its temporary directory.", e);
        } finally {
            if (dir != null) {
                deleteQuietly(dir);
            }
        }
    }

    private static void write(Path file, String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    /** Reads at most {@code limit} bytes as UTF-8, marking the cut when there was more. */
    static String read(Path file, int limit) throws IOException {
        if (!Files.exists(file)) {
            return "";
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] data = in.readNBytes(limit + 1);
            boolean truncated = data.length > limit;
            String text = new String(data, 0, Math.min(data.length, limit), StandardCharsets.UTF_8).replace("\r\n", "\n");
            return truncated ? text + "\n…[output truncated]" : text;
        }
    }

    private static void deleteQuietly(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
                    Files.deleteIfExists(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException | UncheckedIOException ignored) {
            // a leftover temp directory is harmless; the OS cleans it up
        }
    }
}
