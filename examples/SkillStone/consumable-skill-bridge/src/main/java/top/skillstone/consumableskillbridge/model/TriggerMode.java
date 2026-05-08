package top.skillstone.consumableskillbridge.model;

import java.util.Locale;

public enum TriggerMode {
    RIGHT_CLICK,
    LEFT_CLICK,
    SHIFT_RIGHT_CLICK,
    SHIFT_LEFT_CLICK,
    EAT,
    ON_ITEM;

    public static TriggerMode from(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return RIGHT_CLICK;
        }

        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replace('+', '_');
        if ("USE_ON_ITEM".equals(normalized) || "ITEM".equals(normalized) || "ITEM_USE".equals(normalized)) {
            return ON_ITEM;
        }
        if ("LEFT".equals(normalized) || "LEFT_CLICK_AIR".equals(normalized) || "LEFT_CLICK_BLOCK".equals(normalized)) {
            return LEFT_CLICK;
        }
        if ("SHIFT_RIGHT".equals(normalized)
                || "SNEAK_RIGHT".equals(normalized)
                || "SHIFT_RIGHT_CLICK_AIR".equals(normalized)
                || "SHIFT_RIGHT_CLICK_BLOCK".equals(normalized)
                || "SNEAK_RIGHT_CLICK".equals(normalized)) {
            return SHIFT_RIGHT_CLICK;
        }
        if ("SHIFT_LEFT".equals(normalized)
                || "SNEAK_LEFT".equals(normalized)
                || "SHIFT_LEFT_CLICK_AIR".equals(normalized)
                || "SHIFT_LEFT_CLICK_BLOCK".equals(normalized)
                || "SNEAK_LEFT_CLICK".equals(normalized)) {
            return SHIFT_LEFT_CLICK;
        }

        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return RIGHT_CLICK;
        }
    }
}
