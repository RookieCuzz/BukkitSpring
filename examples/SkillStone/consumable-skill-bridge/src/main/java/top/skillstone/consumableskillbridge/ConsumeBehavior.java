package top.skillstone.consumableskillbridge;

import java.util.Locale;

public enum ConsumeBehavior {
    DEFAULT,
    ALWAYS,
    NEVER;

    public static ConsumeBehavior from(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return DEFAULT;
        }

        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return DEFAULT;
        }
    }
}
