package com.pyjudge.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Submitted form fields. Servlets wrap the HTTP request in one of these; tests build them from
 * plain values, so the validation rules in the services can be exercised without a web server.
 */
public interface Form {
    /** All values submitted under {@code name} (checkbox groups, repeated inputs); never null. */
    List<String> values(String name);

    /** The first value, or null when the field was not submitted. */
    default String raw(String name) {
        List<String> all = values(name);
        return all.isEmpty() ? null : all.get(0);
    }

    /** The first value, or an empty string. */
    default String get(String name) {
        String value = raw(name);
        return value == null ? "" : value;
    }

    default String trimmed(String name) {
        return get(name).strip();
    }

    /** True for a ticked checkbox (any non-empty value). */
    default boolean checked(String name) {
        return !get(name).isEmpty();
    }

    /** The value as a whole number, or null when it is missing or not a number. */
    default Integer integer(String name) {
        try {
            return Integer.valueOf(get(name).strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** {@code Form.of("title", "Two Sum", "tags", "arrays")}; repeat a name to give it several values. */
    static Form of(String... pairs) {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            fields.computeIfAbsent(pairs[i], k -> new ArrayList<>()).add(pairs[i + 1]);
        }
        return name -> fields.getOrDefault(name, List.of());
    }

    static Form of(Map<String, String[]> parameters) {
        return name -> {
            String[] values = parameters.get(name);
            return values == null ? List.of() : Arrays.asList(values);
        };
    }
}
