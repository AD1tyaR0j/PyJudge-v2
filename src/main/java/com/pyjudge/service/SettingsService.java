package com.pyjudge.service;

import com.pyjudge.dao.SettingDao;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** System-wide settings, stored as key/value rows and edited by admins. */
public class SettingsService {
    public enum Kind { TEXT, BOOL, INT }

    /** One setting: its type, default, form label and (for numbers) allowed range. */
    public static final class Definition {
        private final String key;
        private final Kind kind;
        private final Object defaultValue;
        private final String label;
        private final String help;
        private final int min;
        private final int max;

        private Definition(String key, Kind kind, Object defaultValue, String label, String help, int min, int max) {
            this.key = key;
            this.kind = kind;
            this.defaultValue = defaultValue;
            this.label = label;
            this.help = help;
            this.min = min;
            this.max = max;
        }

        public String getKey() { return key; }
        public Kind getKind() { return kind; }
        public Object getDefaultValue() { return defaultValue; }
        public String getLabel() { return label; }
        public String getHelp() { return help; }
        public int getMin() { return min; }
        public int getMax() { return max; }
    }

    public static final Map<String, Definition> SCHEMA;

    static {
        Map<String, Definition> schema = new LinkedHashMap<>();
        add(schema, "site_name", Kind.TEXT, "PyJudge", "Site name", "Shown in the header and page titles.", 0, 0);
        add(schema, "announcement", Kind.TEXT, "", "Announcement banner",
                "Shown at the top of every page. Leave empty to hide.", 0, 0);
        add(schema, "registration_open", Kind.BOOL, true, "Open registration",
                "Let new students sign up on their own.", 0, 0);
        add(schema, "require_approval", Kind.BOOL, true, "Require exercise approval",
                "Instructor exercises must be approved by an admin before students can see them.", 0, 0);
        add(schema, "messaging_enabled", Kind.BOOL, true, "Enable messaging",
                "Let students and instructors message each other.", 0, 0);
        add(schema, "leaderboard_enabled", Kind.BOOL, true, "Show leaderboard", "Show the public leaderboard page.", 0, 0);
        add(schema, "default_time_limit", Kind.INT, 2, "Default time limit (s)",
                "Used for new exercises and the Run button.", 1, 10);
        add(schema, "default_memory_limit", Kind.INT, 128, "Default memory limit (MB)",
                "Used for new exercises and the Run button.", 16, 1024);
        add(schema, "max_pending_submissions", Kind.INT, 3, "Max submissions in the queue per student",
                "Stops one student from flooding the judge.", 1, 20);
        SCHEMA = Collections.unmodifiableMap(schema);
    }

    private static void add(Map<String, Definition> schema, String key, Kind kind, Object def, String label,
                            String help, int min, int max) {
        schema.put(key, new Definition(key, kind, def, label, help, min, max));
    }

    private final SettingDao dao;
    /** Settings change rarely and are read on every request, so the typed map is cached until the next save. */
    private volatile Map<String, Object> cache;

    public SettingsService(SettingDao dao) {
        this.dao = dao;
    }

    /** All settings as typed values (String, Boolean or Integer), with defaults for anything not stored. */
    public Map<String, Object> all() {
        Map<String, Object> current = cache;
        if (current == null) {
            Map<String, String> stored = dao.all();
            Map<String, Object> values = new LinkedHashMap<>();
            for (Definition def : SCHEMA.values()) {
                values.put(def.key, decode(def, stored.get(def.key)));
            }
            current = Collections.unmodifiableMap(values);
            cache = current;
        }
        return current;
    }

    private static Object decode(Definition def, String raw) {
        if (raw == null) {
            return def.defaultValue;
        }
        try {
            return switch (def.kind) {
                case BOOL -> "1".equals(raw);
                case INT -> Integer.valueOf(raw);
                case TEXT -> raw;
            };
        } catch (NumberFormatException e) {
            return def.defaultValue;
        }
    }

    public String text(String key) {
        return (String) all().get(key);
    }

    public boolean flag(String key) {
        return (Boolean) all().get(key);
    }

    public int number(String key) {
        return (Integer) all().get(key);
    }

    /** Validates and saves settings from a submitted form. Returns the validation errors (empty on success). */
    public List<String> update(Form form) {
        List<String> errors = new ArrayList<>();
        Map<String, String> values = new LinkedHashMap<>();
        for (Definition def : SCHEMA.values()) {
            switch (def.kind) {
                case BOOL -> values.put(def.key, form.checked(def.key) ? "1" : "0");
                case INT -> {
                    Integer value = form.integer(def.key);
                    if (value == null) {
                        errors.add(def.label + " must be a whole number.");
                    } else if (value < def.min || value > def.max) {
                        errors.add(def.label + " must be between " + def.min + " and " + def.max + ".");
                    } else {
                        values.put(def.key, value.toString());
                    }
                }
                case TEXT -> {
                    String value = form.trimmed(def.key);
                    if (def.key.equals("site_name") && value.isEmpty()) {
                        errors.add("Site name can't be empty.");
                    } else {
                        values.put(def.key, value.length() > 500 ? value.substring(0, 500) : value);
                    }
                }
            }
        }
        if (errors.isEmpty()) {
            dao.saveAll(values);
            cache = null;
        }
        return errors;
    }

    /** Sets one value directly (tests and the command line). */
    public void set(String key, String rawValue) {
        dao.saveAll(Map.of(key, rawValue));
        cache = null;
    }
}
