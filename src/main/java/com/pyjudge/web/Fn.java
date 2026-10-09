package com.pyjudge.web;

import com.pyjudge.service.MarkdownService;
import com.pyjudge.util.Times;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/** Functions the JSP pages call through the {@code pj:} prefix (declared in WEB-INF/pyjudge.tld). */
public final class Fn {
    private static final MarkdownService MARKDOWN = new MarkdownService();

    private Fn() {
    }

    /** "2026-03-01 14:05", or an empty string for null. */
    public static String dt(LocalDateTime value) {
        return value == null ? "" : Times.DISPLAY.format(value);
    }

    /** "March 2026". */
    public static String monthYear(LocalDateTime value) {
        return value == null ? "" : Times.MONTH_YEAR.format(value);
    }

    /** The value format of an HTML datetime-local input. */
    public static String formDt(LocalDateTime value) {
        return value == null ? "" : Times.FORM.format(value);
    }

    /** Markdown to HTML. The result is safe to print unescaped: raw HTML in the source is escaped. */
    public static String md(String text) {
        return MARKDOWN.render(text);
    }

    /** JSON that is safe inside a script element. */
    public static String json(Object value) {
        return Json.stringify(value);
    }

    /** Percent-encodes a value for use in a URL query string. */
    public static String url(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
