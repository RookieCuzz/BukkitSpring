package com.monstercontroller.bukkitspring.logging;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class LokiSettings {
    public enum DropPolicy {
        DROP_NEWEST,
        DROP_OLDEST;

        public static DropPolicy fromString(String value) {
            if (value == null) {
                return DROP_NEWEST;
            }
            String normalized = value.trim().toLowerCase();
            if ("drop-oldest".equals(normalized) || "oldest".equals(normalized)) {
                return DROP_OLDEST;
            }
            return DROP_NEWEST;
        }
    }

    public final boolean enabled;
    public final boolean useServerLogger;
    public final boolean useRootLogger;
    public final String url;
    public final String tenantId;
    public final int maxBatchSize;
    public final long maxWaitMillis;
    public final int queueCapacity;
    public final long connectTimeoutMillis;
    public final long requestTimeoutMillis;
    public final DropPolicy dropPolicy;
    public final boolean includeLoggerLabel;
    public final boolean includeLevelLabel;
    public final boolean includePluginLabel;
    public final String serverLabel;
    public final Map<String, String> staticLabels;
    public final boolean debug;

    private LokiSettings(boolean enabled,
                         boolean useServerLogger,
                         boolean useRootLogger,
                         String url,
                         String tenantId,
                         int maxBatchSize,
                         long maxWaitMillis,
                         int queueCapacity,
                         long connectTimeoutMillis,
                         long requestTimeoutMillis,
                         DropPolicy dropPolicy,
                         boolean includeLoggerLabel,
                         boolean includeLevelLabel,
                         boolean includePluginLabel,
                         String serverLabel,
                         Map<String, String> staticLabels,
                         boolean debug) {
        this.enabled = enabled;
        this.useServerLogger = useServerLogger;
        this.useRootLogger = useRootLogger;
        this.url = url;
        this.tenantId = tenantId;
        this.maxBatchSize = maxBatchSize;
        this.maxWaitMillis = maxWaitMillis;
        this.queueCapacity = queueCapacity;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.requestTimeoutMillis = requestTimeoutMillis;
        this.dropPolicy = dropPolicy;
        this.includeLoggerLabel = includeLoggerLabel;
        this.includeLevelLabel = includeLevelLabel;
        this.includePluginLabel = includePluginLabel;
        this.serverLabel = serverLabel;
        this.staticLabels = staticLabels;
        this.debug = debug;
    }

    public static LokiSettings fromConfig(FileConfiguration config) {
        boolean enabled = config.getBoolean("loki.enabled", false);
        boolean useServerLogger = config.getBoolean("loki.server-logger", true);
        boolean useRootLogger = config.getBoolean("loki.root-logger", false);
        String url = config.getString("loki.url", "http://localhost:3100/loki/api/v1/push");
        String tenantId = config.getString("loki.tenant-id", "");

        int maxBatchSize = clampInt(config.getInt("loki.batch.max-size", 200), 1, 5000);
        long maxWaitMillis = clampLong(config.getLong("loki.batch.max-wait-ms", 1000), 0, 60000);
        int queueCapacity = clampInt(config.getInt("loki.queue.capacity", 10000), 1, 1000000);
        long connectTimeoutMillis = clampLong(config.getLong("loki.timeouts.connect-ms", 2000), 1, 60000);
        long requestTimeoutMillis = clampLong(config.getLong("loki.timeouts.request-ms", 5000), 1, 120000);
        DropPolicy dropPolicy = DropPolicy.fromString(config.getString("loki.queue.drop-policy", "drop-newest"));

        boolean includeLoggerLabel = config.getBoolean("loki.labels.include-logger", true);
        boolean includeLevelLabel = config.getBoolean("loki.labels.include-level", true);
        boolean includePluginLabel = config.getBoolean("loki.labels.include-plugin", true);
        String serverLabel = config.getString("loki.labels.server", "");
        boolean debug = config.getBoolean("loki.debug", false);

        Map<String, String> staticLabels = new TreeMap<>();
        ConfigurationSection section = config.getConfigurationSection("loki.labels.static");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Object value = section.get(key);
                if (value != null) {
                    staticLabels.put(key, String.valueOf(value));
                }
            }
        }

        return new LokiSettings(
                enabled,
                useServerLogger,
                useRootLogger,
                url,
                tenantId == null ? "" : tenantId.trim(),
                maxBatchSize,
                maxWaitMillis,
                queueCapacity,
                connectTimeoutMillis,
                requestTimeoutMillis,
                dropPolicy,
                includeLoggerLabel,
                includeLevelLabel,
                includePluginLabel,
                serverLabel == null ? "" : serverLabel.trim(),
                Collections.unmodifiableMap(new LinkedHashMap<>(staticLabels)),
                debug
        );
    }

    private static int clampInt(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
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
}
