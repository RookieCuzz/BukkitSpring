package com.cuzz.starter.bukkitspring.prometheus.config;

import com.cuzz.bukkitspring.spi.config.ConfigSection;
import com.cuzz.bukkitspring.spi.config.ConfigView;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prometheus Pushgateway configuration settings.
 */
public final class PrometheusSettings {
    public enum PushMode {
        PUSH,
        PUSH_ADD;

        public static PushMode fromString(String value) {
            if (value == null) {
                return PUSH;
            }
            String normalized = value.trim().toLowerCase();
            if ("push_add".equals(normalized) || "pushadd".equals(normalized) || "add".equals(normalized)) {
                return PUSH_ADD;
            }
            return PUSH;
        }
    }

    public final boolean enabled;
    public final String pushGatewayUrl;
    public final String job;
    public final String instance;
    public final boolean includeJvm;
    public final long pushIntervalMs;
    public final long pushTimeoutMs;
    public final PushMode pushMode;
    public final Map<String, String> grouping;
    public final boolean debug;

    private PrometheusSettings(boolean enabled,
                               String pushGatewayUrl,
                               String job,
                               String instance,
                               boolean includeJvm,
                               long pushIntervalMs,
                               long pushTimeoutMs,
                               PushMode pushMode,
                               Map<String, String> grouping,
                               boolean debug) {
        this.enabled = enabled;
        this.pushGatewayUrl = pushGatewayUrl;
        this.job = job;
        this.instance = instance;
        this.includeJvm = includeJvm;
        this.pushIntervalMs = pushIntervalMs;
        this.pushTimeoutMs = pushTimeoutMs;
        this.pushMode = pushMode;
        this.grouping = grouping;
        this.debug = debug;
    }

    public static PrometheusSettings fromConfig(ConfigView config) {
        ConfigView safeConfig = config == null ? EmptyConfigView.INSTANCE : config;
        boolean enabled = safeConfig.getBoolean("prometheus.enabled", false);
        String pushGatewayUrl = normalizeOrDefault(
                safeConfig.getString("prometheus.pushgateway.url", "http://localhost:9091"),
                "http://localhost:9091"
        );
        String job = normalizeOrDefault(
                safeConfig.getString("prometheus.job", "bukkitspring"),
                "bukkitspring"
        );
        String instance = normalize(safeConfig.getString("prometheus.instance", ""));
        boolean includeJvm = safeConfig.getBoolean("prometheus.include-jvm", true);
        long pushIntervalMs = clampLong(safeConfig.getLong("prometheus.push-interval-ms", 15000), 1000, 600000);
        long pushTimeoutMs = clampLong(safeConfig.getLong("prometheus.push-timeout-ms", 5000), 500, 60000);
        PushMode pushMode = PushMode.fromString(safeConfig.getString("prometheus.push-mode", "push"));
        boolean debug = safeConfig.getBoolean("prometheus.debug", false);

        Map<String, String> grouping = new LinkedHashMap<>();
        ConfigSection section = safeConfig.getSection("prometheus.grouping");
        if (section != null) {
            for (String key : section.keys()) {
                Object value = section.get(key);
                if (value != null) {
                    String normalized = normalize(value.toString());
                    if (!normalized.isEmpty()) {
                        grouping.put(key, normalized);
                    }
                }
            }
        }
        if (!instance.isEmpty()) {
            grouping.put("instance", instance);
        }

        return new PrometheusSettings(
                enabled,
                pushGatewayUrl,
                job,
                instance,
                includeJvm,
                pushIntervalMs,
                pushTimeoutMs,
                pushMode,
                Collections.unmodifiableMap(grouping),
                debug
        );
    }

    public boolean isConfigured() {
        return !pushGatewayUrl.isEmpty() && !job.isEmpty();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String normalizeOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized;
    }

    private static long clampLong(long value, long min, long max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private enum EmptyConfigView implements ConfigView {
        INSTANCE;

        @Override
        public boolean getBoolean(String path, boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public String getString(String path, String defaultValue) {
            return defaultValue;
        }

        @Override
        public int getInt(String path, int defaultValue) {
            return defaultValue;
        }

        @Override
        public long getLong(String path, long defaultValue) {
            return defaultValue;
        }

        @Override
        public ConfigSection getSection(String path) {
            return null;
        }
    }
}
