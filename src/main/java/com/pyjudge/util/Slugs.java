package com.pyjudge.util;

import java.util.Locale;

public final class Slugs {
    private Slugs() {
    }

    /** "Two Sum!" becomes "two-sum". Never returns an empty string. */
    public static String slugify(String text) {
        String slug = (text == null ? "" : text).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "exercise" : slug;
    }
}
