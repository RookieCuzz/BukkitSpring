package com.cuzz.starter.bukkitspring.time.testutil;

import com.cuzz.bukkitspring.spi.config.ConfigSection;
import com.cuzz.bukkitspring.spi.config.ConfigView;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class MapConfigView implements ConfigView {
    private final Map<String, Object> values;

    public MapConfigView(Map<String, Object> values) {
        this.values = values == null ? Collections.emptyMap() : new LinkedHashMap<>(values);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = values.get(path);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = value.toString().trim();
        if ("true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text) || "on".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text) || "0".equals(text) || "no".equalsIgnoreCase(text) || "off".equalsIgnoreCase(text)) {
            return false;
        }
        return defaultValue;
    }

    @Override
    public String getString(String path, String defaultValue) {
        Object value = values.get(path);
        return value == null ? defaultValue : value.toString();
    }

    @Override
    public int getInt(String path, int defaultValue) {
        Object value = values.get(path);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    @Override
    public long getLong(String path, long defaultValue) {
        Object value = values.get(path);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    @Override
    public ConfigSection getSection(String path) {
        String prefix = path + ".";
        Map<String, Object> section = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix) && key.length() > prefix.length()) {
                String trimmed = key.substring(prefix.length());
                if (!trimmed.contains(".")) {
                    section.put(trimmed, entry.getValue());
                }
            }
        }
        if (section.isEmpty()) {
            return null;
        }
        return new MapConfigSection(section);
    }

    private static final class MapConfigSection implements ConfigSection {
        private final Map<String, Object> values;

        private MapConfigSection(Map<String, Object> values) {
            this.values = values;
        }

        @Override
        public Set<String> keys() {
            return values.keySet();
        }

        @Override
        public Object get(String key) {
            return values.get(key);
        }
    }
}
