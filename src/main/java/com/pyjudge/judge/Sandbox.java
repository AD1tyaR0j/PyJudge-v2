package com.pyjudge.judge;

/** Runs untrusted Python code with time, memory and output limits. */
public interface Sandbox {
    /**
     * Runs {@code code} once with {@code stdin} as its input.
     *
     * @param timeLimitSeconds wall-clock limit
     * @param memoryLimitMb    address-space limit
     * @param maxOutputBytes   stdout/stderr beyond this are cut off
     */
    RunResult run(String code, String stdin, int timeLimitSeconds, int memoryLimitMb, int maxOutputBytes);

    /** A SyntaxError message, or null if the code compiles. Never executes the code. */
    String checkSyntax(String code);
}
