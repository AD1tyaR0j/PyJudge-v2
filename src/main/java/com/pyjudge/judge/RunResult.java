package com.pyjudge.judge;

/**
 * Outcome of running a program once.
 *
 * @param exitCode null when the run was killed for exceeding the time limit
 */
public record RunResult(RunStatus status, String stdout, String stderr, Integer exitCode, int timeMs) {
}
