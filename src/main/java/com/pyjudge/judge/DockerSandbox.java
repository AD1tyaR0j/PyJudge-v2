package com.pyjudge.judge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Runs each execution in a fresh container with no network, a read-only file system,
 * a non-root user, and memory, CPU and PID caps. Use this for any public deployment.
 */
public class DockerSandbox extends AbstractSandbox {
    private static final int STARTUP_GRACE_SECONDS = 3;
    private static final int OOM_KILLED_EXIT = 137;

    private final String image;

    public DockerSandbox(String image) {
        this.image = image;
    }

    @Override
    protected List<String> command(Path dir, String script, int timeLimitSeconds, int memoryLimitMb, String runId) {
        return List.of(
                "docker", "run", "--rm", "-i", "--name", runId,
                "--network=none",
                "--memory=" + memoryLimitMb + "m", "--memory-swap=" + memoryLimitMb + "m",
                "--cpus=0.5", "--pids-limit=64",
                "--read-only", "--tmpfs", "/tmp:size=16m",
                "--cap-drop=ALL", "--security-opt=no-new-privileges",
                "--user", "65534:65534",
                "-v", dir + ":/code:ro", "-w", "/code",
                image, "python3", "-I", "-X", "utf8", script);
    }

    @Override
    protected Map<String, String> environment() {
        return Map.copyOf(System.getenv()); // the docker client needs PATH, DOCKER_HOST, HOME...
    }

    @Override
    protected int waitSeconds(int timeLimitSeconds) {
        return timeLimitSeconds + STARTUP_GRACE_SECONDS;
    }

    @Override
    protected void onTimeout(String runId) {
        try {
            new ProcessBuilder("docker", "kill", runId).redirectErrorStream(true).start().waitFor();
        } catch (IOException e) {
            // the client process is killed next either way
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected RunStatus classify(boolean timedOut, int exitCode, String stderr) {
        if (timedOut) {
            return RunStatus.TLE;
        }
        if (stderr.contains("MemoryError") || exitCode == OOM_KILLED_EXIT) {
            return RunStatus.MLE;
        }
        return exitCode == 0 ? RunStatus.OK : RunStatus.RE;
    }
}
