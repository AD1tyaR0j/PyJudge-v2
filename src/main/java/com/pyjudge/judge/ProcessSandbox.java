package com.pyjudge.judge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs code in a separate {@code python -I} process inside a throwaway directory with a stripped
 * environment. On Linux and macOS the process also gets {@code ulimit} caps on memory, CPU time
 * and file size. Fine for local development; NOT a security boundary for a public deployment
 * (use {@link DockerSandbox} there).
 */
public class ProcessSandbox extends AbstractSandbox {
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private static final int SIGXCPU_EXIT = 128 + 24;
    /** Applies the limits, then replaces the shell with the real command so signals and exit codes pass through. */
    private static final String LIMIT_SCRIPT =
            "ulimit -v \"$1\" 2>/dev/null; ulimit -t \"$2\" 2>/dev/null; ulimit -f 65536 2>/dev/null;"
                    + " ulimit -c 0 2>/dev/null; shift 2; exec \"$@\"";

    private final String configuredPython;
    private volatile String python;

    /** @param pythonExecutable the interpreter to use, or empty to look for python3 / python on the PATH */
    public ProcessSandbox(String pythonExecutable) {
        this.configuredPython = pythonExecutable == null ? "" : pythonExecutable.strip();
    }

    @Override
    protected List<String> command(Path dir, String script, int timeLimitSeconds, int memoryLimitMb, String runId) {
        List<String> cmd = new ArrayList<>();
        if (!WINDOWS && new File("/bin/sh").canExecute()) {
            cmd.addAll(List.of("/bin/sh", "-c", LIMIT_SCRIPT, "sh",
                    String.valueOf(memoryLimitMb * 1024L), String.valueOf(timeLimitSeconds + 1)));
        }
        // -I: isolated mode (ignores PYTHON* variables and user site-packages)
        // -X utf8: isolated mode ignores PYTHONIOENCODING, and Windows would otherwise use cp1252
        cmd.addAll(List.of(python(), "-I", "-X", "utf8", script));
        return cmd;
    }

    @Override
    protected Map<String, String> environment() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("PYTHONIOENCODING", "utf-8");
        env.put("PYTHONDONTWRITEBYTECODE", "1");
        for (String key : List.of("PATH", "SYSTEMROOT", "TEMP", "TMP")) { // Python on Windows needs SYSTEMROOT
            String value = System.getenv(key);
            if (value != null) {
                env.put(key, value);
            }
        }
        return env;
    }

    @Override
    protected RunStatus classify(boolean timedOut, int exitCode, String stderr) {
        if (timedOut) {
            return RunStatus.TLE;
        }
        if (stderr.contains("MemoryError")) {
            return RunStatus.MLE;
        }
        if (!WINDOWS && exitCode == SIGXCPU_EXIT) { // CPU ulimit hit
            return RunStatus.TLE;
        }
        return exitCode == 0 ? RunStatus.OK : RunStatus.RE;
    }

    /** The interpreter to run, found once and remembered. */
    private String python() {
        String found = python;
        if (found == null) {
            found = !configuredPython.isEmpty() ? configuredPython : detect();
            python = found;
        }
        return found;
    }

    private static String detect() {
        for (String candidate : WINDOWS ? List.of("python", "py", "python3") : List.of("python3", "python")) {
            try {
                Process p = new ProcessBuilder(candidate, "--version").redirectErrorStream(true).start();
                p.getInputStream().readAllBytes();
                if (p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return candidate;
                }
                p.destroyForcibly();
            } catch (IOException e) {
                // not on the PATH: try the next name
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return WINDOWS ? "python" : "python3";
    }
}
