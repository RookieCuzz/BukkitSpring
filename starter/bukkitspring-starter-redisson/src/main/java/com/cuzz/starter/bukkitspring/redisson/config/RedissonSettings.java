package com.cuzz.starter.bukkitspring.redisson.config;

import com.cuzz.bukkitspring.spi.config.ConfigSection;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.starter.bukkitspring.redisson.api.RedissonMode;
import org.redisson.config.TransportMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Redisson configuration settings.
 */
public final class RedissonSettings {
    public final boolean enabled;
    public final boolean useVirtualThreads;
    public final RedissonMode mode;
    public final String address;
    public final String username;
    public final String password;
    public final int database;
    public final String clientName;
    public final int timeoutMillis;
    public final int connectTimeoutMillis;
    public final int idleConnectionTimeoutMillis;
    public final int retryAttempts;
    public final int retryIntervalMillis;
    public final int connectionPoolSize;
    public final int connectionMinimumIdleSize;
    public final int subscriptionConnectionPoolSize;
    public final int subscriptionConnectionMinimumIdleSize;
    public final int threads;
    public final int nettyThreads;
    public final TransportMode transportMode;
    public final RedissonClusterSettings cluster;

    private RedissonSettings(boolean enabled,
                             boolean useVirtualThreads,
                             RedissonMode mode,
                             String address,
                             String username,
                             String password,
                             int database,
                             String clientName,
                             int timeoutMillis,
                             int connectTimeoutMillis,
                             int idleConnectionTimeoutMillis,
                             int retryAttempts,
                             int retryIntervalMillis,
                             int connectionPoolSize,
                             int connectionMinimumIdleSize,
                             int subscriptionConnectionPoolSize,
                             int subscriptionConnectionMinimumIdleSize,
                             int threads,
                             int nettyThreads,
                             TransportMode transportMode,
                             RedissonClusterSettings cluster) {
        this.enabled = enabled;
        this.useVirtualThreads = useVirtualThreads;
        this.mode = mode;
        this.address = address;
        this.username = username;
        this.password = password;
        this.database = database;
        this.clientName = clientName;
        this.timeoutMillis = timeoutMillis;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.idleConnectionTimeoutMillis = idleConnectionTimeoutMillis;
        this.retryAttempts = retryAttempts;
        this.retryIntervalMillis = retryIntervalMillis;
        this.connectionPoolSize = connectionPoolSize;
        this.connectionMinimumIdleSize = connectionMinimumIdleSize;
        this.subscriptionConnectionPoolSize = subscriptionConnectionPoolSize;
        this.subscriptionConnectionMinimumIdleSize = subscriptionConnectionMinimumIdleSize;
        this.threads = threads;
        this.nettyThreads = nettyThreads;
        this.transportMode = transportMode;
        this.cluster = cluster;
    }

    public static RedissonSettings fromConfig(ConfigView config) {
        ConfigView safeConfig = config == null ? EmptyConfigView.INSTANCE : config;

        boolean enabled = safeConfig.getBoolean("redisson.enabled", false);
        boolean useVirtualThreads = safeConfig.getBoolean("redisson.virtual-threads", true);
        RedissonMode mode = RedissonMode.fromString(safeConfig.getString("redisson.mode", "single"));

        String address = normalizeAddress(safeConfig.getString("redisson.address", "redis://127.0.0.1:6379"));
        String username = normalize(safeConfig.getString("redisson.username", ""));
        String password = normalize(safeConfig.getString("redisson.password", ""));
        int database = clampInt(safeConfig.getInt("redisson.database", 0), 0, 255);
        String clientName = normalize(safeConfig.getString("redisson.client-name", "bukkitspring-redisson"));

        int timeoutMillis = clampInt(safeConfig.getInt("redisson.timeout-ms", 3000), 1, 120_000);
        int connectTimeoutMillis = clampInt(safeConfig.getInt("redisson.connect-timeout-ms", 10_000), 1, 120_000);
        int idleConnectionTimeoutMillis = clampInt(safeConfig.getInt("redisson.idle-connection-timeout-ms", 10_000), 1, 120_000);
        int retryAttempts = clampInt(safeConfig.getInt("redisson.retry-attempts", 3), 0, 100);
        int retryIntervalMillis = clampInt(safeConfig.getInt("redisson.retry-interval-ms", 1500), 1, 60_000);

        int connectionPoolSize = clampInt(safeConfig.getInt("redisson.connection-pool-size", 64), 1, 1024);
        int connectionMinimumIdleSize = clampInt(safeConfig.getInt("redisson.connection-minimum-idle-size", 24), 0, 1024);
        int subscriptionConnectionPoolSize = clampInt(safeConfig.getInt("redisson.subscription-connection-pool-size", 50), 1, 1024);
        int subscriptionConnectionMinimumIdleSize = clampInt(safeConfig.getInt("redisson.subscription-connection-minimum-idle-size", 1), 0, 1024);

        int threads = clampInt(safeConfig.getInt("redisson.threads", 0), 0, 1024);
        int nettyThreads = clampInt(safeConfig.getInt("redisson.netty-threads", 0), 0, 1024);
        TransportMode transportMode = parseTransportMode(safeConfig.getString("redisson.transport-mode", "NIO"));

        boolean clusterEnabled = safeConfig.getBoolean("redisson.cluster.enabled", false);
        List<String> clusterNodes = normalizeAddresses(readStringList(
                safeConfig,
                "redisson.cluster",
                "nodes",
                "redisson.cluster.nodes"
        ));
        int scanIntervalMillis = clampInt(safeConfig.getInt("redisson.cluster.scan-interval-ms", 1000), 100, 300_000);
        RedissonClusterSettings cluster = new RedissonClusterSettings(clusterEnabled, clusterNodes, scanIntervalMillis);

        if (cluster.enabled || !cluster.nodes.isEmpty()) {
            mode = RedissonMode.CLUSTER;
        }

        return new RedissonSettings(
                enabled,
                useVirtualThreads,
                mode,
                address,
                username,
                password,
                database,
                clientName,
                timeoutMillis,
                connectTimeoutMillis,
                idleConnectionTimeoutMillis,
                retryAttempts,
                retryIntervalMillis,
                connectionPoolSize,
                connectionMinimumIdleSize,
                subscriptionConnectionPoolSize,
                subscriptionConnectionMinimumIdleSize,
                threads,
                nettyThreads,
                transportMode,
                cluster
        );
    }

    private static List<String> readStringList(ConfigView config, String sectionPath, String key, String fallbackPath) {
        if (config != null) {
            ConfigSection section = config.getSection(sectionPath);
            if (section != null) {
                Object value = section.get(key);
                List<String> list = coerceStringList(value);
                if (value != null || !list.isEmpty()) {
                    return list;
                }
            }
            String fallback = config.getString(fallbackPath, "");
            if (fallback != null && !fallback.isBlank()) {
                return splitList(fallback);
            }
        }
        return List.of();
    }

    private static List<String> coerceStringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> results = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    String normalized = normalize(item.toString());
                    if (!normalized.isEmpty()) {
                        results.add(normalized);
                    }
                }
            }
            return results;
        }
        if (value instanceof String text) {
            return splitList(text);
        }
        return List.of();
    }

    private static List<String> splitList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return List.of();
        }
        String[] parts = value.split(",");
        List<String> results = new ArrayList<>(parts.length);
        for (String part : parts) {
            String normalized = normalize(part);
            if (!normalized.isEmpty()) {
                results.add(normalized);
            }
        }
        return results;
    }

    private static List<String> normalizeAddresses(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> results = new ArrayList<>(values.size());
        for (String value : values) {
            String address = normalizeAddress(value);
            if (!address.isEmpty()) {
                results.add(address);
            }
        }
        return results;
    }

    private static String normalizeAddress(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.contains("://")) {
            return normalized;
        }
        return "redis://" + normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
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

    private static TransportMode parseTransportMode(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return TransportMode.NIO;
        }
        try {
            return TransportMode.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return TransportMode.NIO;
        }
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
