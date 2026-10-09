package com.pyjudge.judge;

import com.pyjudge.config.AppConfig;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.Verdict;
import java.util.ArrayList;
import java.util.List;

/** Compares a program's output with the expected output of each test case. */
public class Judge {
    private final Sandbox sandbox;

    public Judge(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    /** Line endings and trailing whitespace never decide a verdict. */
    public static String normalize(String text) {
        String[] lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        List<String> kept = new ArrayList<>();
        for (String line : lines) {
            kept.add(line.stripTrailing());
        }
        while (!kept.isEmpty() && kept.get(kept.size() - 1).isEmpty()) {
            kept.remove(kept.size() - 1);
        }
        return String.join("\n", kept);
    }

    public static boolean outputsMatch(String actual, String expected) {
        return normalize(actual).equals(normalize(expected));
    }

    /** Judges {@code code} against {@code cases}, stopping at the first failing case like most online judges. */
    public JudgeResult judge(String code, List<TestCase> cases, int timeLimit, int memoryLimit) {
        int total = cases.size();
        if (total == 0) {
            return new JudgeResult(Verdict.IE, 0, 0, 0, "This problem has no test cases yet.");
        }
        String syntaxError = sandbox.checkSyntax(code);
        if (syntaxError != null) {
            return new JudgeResult(Verdict.CE, 0, total, 0, syntaxError);
        }

        int passed = 0;
        int slowest = 0;
        for (int i = 0; i < total; i++) {
            TestCase tc = cases.get(i);
            RunResult run = sandbox.run(code, tc.getInput(), timeLimit, memoryLimit, AppConfig.JUDGE_MAX_OUTPUT_BYTES);
            slowest = Math.max(slowest, run.timeMs());
            String label = (tc.isSample() ? "sample" : "hidden") + " test case " + (i + 1);

            if (run.status() != RunStatus.OK) {
                String detail = switch (run.status()) {
                    case TLE -> "Time limit of " + timeLimit + "s exceeded on " + label + ".";
                    case MLE -> "Memory limit of " + memoryLimit + " MB exceeded on " + label + ".";
                    default -> "Runtime error on " + label + ":\n" + tail(run.stderr().strip(), 2000);
                };
                return new JudgeResult(Verdict.valueOf(run.status().name()), passed, total, slowest, detail);
            }
            if (!outputsMatch(run.stdout(), tc.getExpectedOutput())) {
                String error = "Wrong answer on " + label + ".";
                if (tc.isSample()) { // hidden cases stay hidden
                    error += "\n\nInput:\n" + tc.getInput().strip()
                            + "\n\nExpected:\n" + normalize(tc.getExpectedOutput())
                            + "\n\nYour output:\n" + head(normalize(run.stdout()), 2000);
                }
                return new JudgeResult(Verdict.WA, passed, total, slowest, error);
            }
            passed++;
        }
        return new JudgeResult(Verdict.AC, passed, total, slowest, null);
    }

    private static String head(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String tail(String s, int max) {
        return s.length() <= max ? s : s.substring(s.length() - max);
    }
}
