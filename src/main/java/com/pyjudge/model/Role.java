package com.pyjudge.model;

import java.util.Locale;

/** The three kinds of account. Stored in the database as the lowercase {@link #key()}. */
public enum Role {
    STUDENT("Student"),
    INSTRUCTOR("Instructor"),
    ADMIN("Admin");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String getKey() {
        return key();
    }

    public String getLabel() {
        return label;
    }

    /** Admins and instructors can create exercises, lessons and challenges. */
    public boolean canAuthor() {
        return this != STUDENT;
    }

    /** Parses a database or form value; returns {@code null} for anything unknown. */
    public static Role fromKey(String key) {
        for (Role role : values()) {
            if (role.key().equals(key)) {
                return role;
            }
        }
        return null;
    }
}
