package com.cuzz.starter.bukkitspring.caffeine.testutil;

import com.cuzz.bukkitspring.spi.config.ConfigSection;
import com.cuzz.bukkitspring.spi.config.ConfigView;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MapConfigView implements ConfigView {
    private final Map<String, Object> root;

    public MapConfigView(Map<String, Object> values) {
        this.root = normalizeMap(values);
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = resolvePath(path);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() != 0;
        }
        if (value instanceof String stringValue) {
            String text = stringValue.trim();
            if ("true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text) || "on".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text) || "no".equalsIgnoreCase(text) || "off".equalsIgnoreCase(text)) {
                return false;
            }
        }
        return defaultValue;
    }

    @Override
    public String getString(String path, String defaultValue) {
        Object value = resolvePath(path);
        return value == null ? defaultValue : String.valueOf(value);
    }

    @Override
    public int getInt(String path, int defaultValue) {
        Object value = resolvePath(path);
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Override
    public long getLong(String path, long defaultValue) {
        Object value = resolvePath(path);
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Override
    public ConfigSection getSection(String path) {
        Object value = resolvePath(path);
        if (value instanceof Map<?, ?> mapValue) {
            return new MapConfigSection(normalizeMap(mapValue));
        }
        return null;
    }

    private Object resolvePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> mapValue)) {
                return null;
            }
            current = mapValue.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static Map<String, Object> normalizeMap(Map<?, ?> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = Objects.toString(entry.getKey(), null);
            if (key == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                value = normalizeMap(nested);
            }
            normalized.put(key, value);
        }
        return normalized;
    }

    private static final class MapConfigSection implements ConfigSection {
        private final Map<String, Object> section;

        private MapConfigSection(Map<String, Object> section) {
            this.section = section;
        }

        @Override
        public Set<String> keys() {
            return section.keySet();
        }

        @Override
        public Object get(String key) {
            return section.get(key);
        }
    }
}
