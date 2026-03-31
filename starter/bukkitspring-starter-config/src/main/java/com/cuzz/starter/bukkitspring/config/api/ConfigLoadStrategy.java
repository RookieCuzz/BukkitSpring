package com.cuzz.starter.bukkitspring.config.api;

/**
 * Source strategy when loading config documents.
 */
public enum ConfigLoadStrategy {
    LOCAL_ONLY,
    REMOTE_ONLY,
    LOCAL_FIRST,
    REMOTE_FIRST;

    public static ConfigLoadStrategy fromString(String raw, ConfigLoadStrategy fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            String normalized = raw.trim()
                    .replace('-', '_')
                    .replace(' ', '_')
                    .toUpperCase();
            return ConfigLoadStrategy.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
