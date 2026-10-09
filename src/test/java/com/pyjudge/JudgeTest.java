package com.pyjudge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pyjudge.config.AppConfig;
import com.pyjudge.judge.Judge;
import com.pyjudge.judge.JudgeResult;
import com.pyjudge.judge.ProcessSandbox;
import com.pyjudge.judge.RunResult;
import com.pyjudge.judge.RunStatus;
import com.pyjudge.judge.Sandbox;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.Verdict;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/** The judge and the process sandbox, exercised with real Python programs. */
class JudgeTest {
    private static final String ECHO_SUM = "a, b = map(int, input().split())\nprint(a + b)\n";
    private static final List<TestCase> CASES = List.of(new TestCase("1 2\n", "3\n", true), new TestCase("10 20\n", "30\n", false));

    private final Sandbox sandbox = new ProcessSandbox("");
    private final Judge judge = new Judge(sandbox);

    private RunResult run(String code, String stdin) {
        return sandbox.run(code, stdin, 2, 128, AppConfig.MAX_OUTPUT_BYTES);
    }

    @Test
    void outputsMatchIgnoresTrailingWhitespace() {
        assertTrue(Judge.outputsMatch("1 2 \n3\n\n", "1 2\n3"));
        assertTrue(Judge.outputsMatch("a\r\nb\r\n", "a\nb\n"));
        assertFalse(Judge.outputsMatch("1  2", "1 2"));
        assertFalse(Judge.outputsMatch(" 1", "1"));
    }

    @Test
    void checkSyntax() {
        assertNull(sandbox.checkSyntax("print('ok')\n"));
        String error = sandbox.checkSyntax("def broken(:\n    pass\n");
        assertNotNull(error);
        assertTrue(error.contains("SyntaxError"), error);
    }

    @Test
    void accepted() {
        JudgeResult result = judge.judge(ECHO_SUM, CASES, 2, 128);
        assertEquals(Verdict.AC, result.verdict());
        assertEquals(2, result.passed());
        assertNull(result.error());
    }

    @Test
    void wrongAnswerShowsDetailsOnlyForSamples() {
        JudgeResult onSample = judge.judge("print(0)\n", CASES, 2, 128);
        assertEquals(Verdict.WA, onSample.verdict());
        assertEquals(0, onSample.passed());
        assertTrue(onSample.error().contains("Expected:\n3"), onSample.error());

        // Right on the sample, wrong on the hidden case: the hidden data must not leak
        JudgeResult onHidden = judge.judge("a, b = map(int, input().split())\nprint(3 if a == 1 else 0)\n", CASES, 2, 128);
        assertEquals(Verdict.WA, onHidden.verdict());
        assertEquals(1, onHidden.passed());
        assertEquals("Wrong answer on hidden test case 2.", onHidden.error());
    }

    @Test
    void syntaxError() {
        JudgeResult result = judge.judge("print('unclosed\n", CASES, 2, 128);
        assertEquals(Verdict.CE, result.verdict());
        assertTrue(result.error().contains("SyntaxError"), result.error());
    }

    @Test
    void runtimeError() {
        JudgeResult result = judge.judge("print(1 // 0)\n", CASES, 2, 128);
        assertEquals(Verdict.RE, result.verdict());
        assertTrue(result.error().contains("ZeroDivisionError"), result.error());
    }

    @Test
    void noTestCases() {
        JudgeResult result = judge.judge(ECHO_SUM, List.of(), 2, 128);
        assertEquals(Verdict.IE, result.verdict());
    }

    @Test
    void infiniteLoopIsTimeLimitExceeded() {
        JudgeResult result = judge.judge("while True:\n    pass\n", CASES, 1, 128);
        assertEquals(Verdict.TLE, result.verdict());
    }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "memory limits need ulimit")
    void hugeAllocationIsMemoryLimitExceeded() {
        JudgeResult result = judge.judge("x = bytearray(1024 * 1024 * 1024)\nprint(len(x))\n", CASES, 2, 64);
        assertEquals(Verdict.MLE, result.verdict());
    }

    @Test
    void runsInThrowawayDirectoryWithStrippedEnvironment() {
        RunResult result = run("import os\nprint(sorted(os.listdir('.')))\nprint(sorted(k for k in os.environ if 'PYJUDGE' in k or 'JAVA' in k))\n", "");
        assertEquals(RunStatus.OK, result.status(), result.stderr());
        assertTrue(result.stdout().contains("main.py"), result.stdout());
        assertTrue(result.stdout().strip().endsWith("[]"), result.stdout());
    }

    @Test
    void tracebacksDoNotRevealTheTempDirectory() {
        RunResult result = run("raise ValueError('boom')\n", "");
        assertEquals(RunStatus.RE, result.status());
        assertTrue(result.stderr().contains("File \"main.py\""), result.stderr());
        assertFalse(result.stderr().contains("judge_"), result.stderr());
    }

    @Test
    void outputIsTruncated() {
        RunResult result = sandbox.run("print('x' * 100000)\n", "", 2, 128, 1000);
        assertEquals(RunStatus.OK, result.status());
        assertTrue(result.stdout().endsWith("[output truncated]"));
        assertTrue(result.stdout().length() < 1100);
    }

    @Test
    void stdinIsPassed() {
        assertEquals("HELLO\n", run("print(input().upper())\n", "hello\n").stdout());
    }

    @Test
    void judgingAcceptsLargeCorrectOutput() {
        String code = "print('\\n'.join(str(i) for i in range(200000)))\n";
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 200000; i++) {
            expected.append(i).append('\n');
        }
        JudgeResult result = judge.judge(code, List.of(new TestCase("", expected.toString(), false)), 5, 256);
        assertEquals(Verdict.AC, result.verdict(), result.error());
    }

    @Test
    void unicodeInputAndOutput() {
        RunResult result = run("print(input()[::-1])\n", "héllo wörld ✔\n");
        assertEquals(RunStatus.OK, result.status(), result.stderr());
        assertEquals("✔ dlröw olléh\n", result.stdout());
    }
}
