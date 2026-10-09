package com.pyjudge.model;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Result of judging a submission. Stored by {@link #name()}. */
public enum Verdict {
    AC("Accepted"),
    WA("Wrong Answer"),
    TLE("Time Limit Exceeded"),
    MLE("Memory Limit Exceeded"),
    RE("Runtime Error"),
    CE("Syntax Error"),
    IE("Internal Error"),
    PENDING("Judging…");

    private final String label;

    Verdict(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** CSS suffix: {@code ac}, {@code wa}... */
    public String getKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Verdict fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return IE;
        }
    }

    /** {@code {"AC": "Accepted", ...}} for the browser. */
    public static Map<String, String> labels() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Verdict v : values()) {
            out.put(v.name(), v.label);
        }
        return out;
    }
}
