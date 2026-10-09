package com.pyjudge.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Application configuration: defaults from {@code application.properties}, overridden by
 * {@code -Dpyjudge.<key>} system properties and {@code PYJUDGE_<KEY>} environment variables.
 */
public final class AppConfig {
    public static final int MAX_CODE_LENGTH = 64 * 1024;
    public static final int MAX_INPUT_LENGTH = 64 * 1024;
    /** Output shown to the user after Run. */
    public static final int MAX_OUTPUT_BYTES = 64 * 1024;
    /** Output compared when judging (big tests legitimately print megabytes). */
    public static final int JUDGE_MAX_OUTPUT_BYTES = 16 * 1024 * 1024;

    private final Properties values = new Properties();

    private AppConfig() {
    }

    /** Loads the bundled defaults and applies system-property and environment overrides. */
    public static AppConfig load() {
        AppConfig config = new AppConfig();
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                config.values.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read application.properties", e);
        }
        return config;
    }

    /** The bundled defaults with the given values on top (used by tests and the command line). */
    public static AppConfig of(Map<String, String> overrides) {
        AppConfig config = load();
        config.values.putAll(overrides);
        return config;
    }

    public String get(String key) {
        String sys = System.getProperty("pyjudge." + key);
        if (sys != null && !sys.isBlank()) {
            return sys.trim();
        }
        String env = System.getenv("PYJUDGE_" + key.toUpperCase(Locale.ROOT).replace('.', '_'));
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return values.getProperty(key, "").trim();
    }

    public String get(String key, String fallback) {
        String value = get(key);
        return value.isEmpty() ? fallback : value;
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean getBoolean(String key, boolean fallback) {
        String value = get(key);
        return value.isEmpty() ? fallback : Boolean.parseBoolean(value);
    }
}
