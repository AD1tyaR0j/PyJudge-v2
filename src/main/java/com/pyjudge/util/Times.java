package com.pyjudge.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Every timestamp in the app is a UTC {@link LocalDateTime}, stored in DATETIME columns. */
public final class Times {
    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter FORM = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    public static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
    public static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private Times() {
    }

    /** Current UTC time, truncated to seconds so values round-trip through every database unchanged. */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }
}
