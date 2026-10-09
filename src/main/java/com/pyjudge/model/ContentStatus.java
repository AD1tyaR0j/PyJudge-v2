package com.pyjudge.model;

import java.util.Locale;

/** Approval state of instructor-written content (exercises and lessons). */
public enum ContentStatus {
    PENDING("⏳ Pending"),
    APPROVED("✔ Approved"),
    REJECTED("✖ Rejected");

    private final String badge;

    ContentStatus(String badge) {
        this.badge = badge;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String getKey() {
        return key();
    }

    public String getBadge() {
        return badge;
    }

    /** "Pending", "Approved", "Rejected". */
    public String getLabel() {
        return name().charAt(0) + key().substring(1);
    }

    public static ContentStatus fromKey(String key) {
        for (ContentStatus status : values()) {
            if (status.key().equals(key)) {
                return status;
            }
        }
        return null;
    }
}
