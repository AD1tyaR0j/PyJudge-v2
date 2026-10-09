package com.pyjudge.model;

import java.util.Locale;

/** Exercise difficulty and the leaderboard points a solve is worth. Stored as the {@link #label()}. */
public enum Difficulty {
    EASY("Easy", 10),
    MEDIUM("Medium", 20),
    HARD("Hard", 40);

    private final String label;
    private final int points;

    Difficulty(String label, int points) {
        this.label = label;
        this.points = points;
    }

    public String label() {
        return label;
    }

    public String getLabel() {
        return label;
    }

    public int getPoints() {
        return points;
    }

    /** CSS suffix: {@code easy}, {@code medium}, {@code hard}. */
    public String getKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Difficulty fromLabel(String label) {
        for (Difficulty d : values()) {
            if (d.label.equals(label)) {
                return d;
            }
        }
        return null;
    }
}
