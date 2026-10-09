package com.pyjudge.judge;

import com.pyjudge.model.Verdict;

/** The verdict for a whole submission. */
public record JudgeResult(Verdict verdict, int passed, int total, Integer runtimeMs, String error) {
}
