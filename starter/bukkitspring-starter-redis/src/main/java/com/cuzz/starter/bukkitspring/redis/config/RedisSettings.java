package com.cuzz.starter.bukkitspring.redis.config;

import com.cuzz.starter.bukkitspring.redis.api.RedisMode;
import com.cuzz.bukkitspring.spi.config.ConfigSection;
import com.cuzz.bukkitspring.spi.config.ConfigView;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis configuration settings.
 */
public final class RedisSettings {
    public final boolean enabled;
    public final boolean useVirtualThreads;
    public final RedisMode mode;
    public final String host;
    public final int port;
    public final String user;
    public final String password;
    public final int database;
    public final boolean ssl;
    public final int connectionTimeoutMillis;
    public final int socketTimeoutMillis;
    public final String clientName;
    public final int poolMaxTotal;
    public final int poolMaxIdle;
    public final int poolMinIdle;
    public final long poolMaxWaitMillis;
    public final RedisClusterSettings cluster;

    private RedisSettings(boolean enabled,
                          boolean useVirtualThreads,
                          RedisMode mode,
                          String host,
                          int port,
                          String user,
                          String password,
                          int database,
                          boolean ssl,
                          int connectionTimeoutMillis,
                          int socketTimeoutMillis,
                          String clientName,
                          int poolMaxTotal,
                          int poolMaxIdle,
                          int poolMinIdle,
                          long poolMaxWaitMillis,
                          RedisClusterSettings cluster) {
        this.enabled = enabled;
        this.useVirtualThreads = useVirtualThreads;
        this.mode = mode;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.database = database;
        this.ssl = ssl;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.socketTimeoutMillis = socketTimeoutMillis;
        this.clientName = clientName;
        this.poolMaxTotal = poolMaxTotal;
        this.poolMaxIdle = poolMaxIdle;
        this.poolMinIdle = poolMinIdle;
        this.poolMaxWaitMillis = poolMaxWaitMillis;
        this.cluster = cluster;
    }

    public static RedisSettings fromConfig(ConfigView config) {
        ConfigView safeConfig = config == null ? EmptyConfigView.INSTANCE : config;
        boolean enabled = safeConfig.getBoolean("redis.enabled", false);
        boolean useVirtualThreads = safeConfig.getBoolean("redis.virtual-threads", true);
        String modeValue = normalize(safeConfig.getString("redis.mode", "standalone"));
        RedisMode mode = RedisMode.fromString(modeValue);

        String host = normalizeOrDefault(safeConfig.getString("redis.host", ""), "localhost");
        int port = clampInt(safeConfig.getInt("redis.port", 6379), 1, 65535);
        String user = normalize(safeConfig.getString("redis.user", ""));
        String password = normalize(safeConfig.getString("redis.password", ""));
        int database = clampInt(safeConfig.getInt("redis.database", 0), 0, 15);
        boolean ssl = safeConfig.getBoolean("redis.ssl", false);
        int connectionTimeoutMillis = clampInt(safeConfig.getInt("redis.timeouts.connect-ms", 2000), 1, 60000);
        int socketTimeoutMillis = clampInt(safeConfig.getInt("redis.timeouts.socket-ms", 2000), 1, 60000);
        String clientName = normalize(safeConfig.getString("redis.client-name", "bukkitspring"));

        int poolMaxTotal = clampInt(safeConfig.getInt("redis.pool.max-total", 16), 1, 512);
        int poolMaxIdle = clampInt(safeConfig.getInt("redis.pool.max-idle", 16), 0, 512);
        int poolMinIdle = clampInt(safeConfig.getInt("redis.pool.min-idle", 0), 0, 512);
        long poolMaxWaitMillis = clampLong(safeConfig.getLong("redis.pool.max-wait-ms", 3000), 1, 60000);

        boolean clusterEnabled = safeConfig.getBoolean("redis.cluster.enabled", false);
        List<String> nodes = normalizeNodes(readStringList(safeConfig, "redis.cluster", "nodes", "redis.cluster.nodes"));
        int maxRedirects = clampInt(safeConfig.getInt("redis.cluster.max-redirects", 5), 1, 20);
        long topologyRefreshMillis = clampLong(safeConfig.getLong("redis.cluster.topology-refresh-ms", 0), 0, 600000);

        if (clusterEnabled || !nodes.isEmpty()) {
            mode = RedisMode.CLUSTER;
        }

        RedisClusterSettings cluster = new RedisClusterSettings(
                clusterEnabled,
                nodes,
                maxRedirects,
                topologyRefreshMillis
        );

        return new RedisSettings(
                enabled,
                useVirtualThreads,
                mode,
                host,
                port,
                user,
                password,
                database,
                ssl,
                connectionTimeoutMillis,
                socketTimeoutMillis,
                clientName,
                poolMaxTotal,
                poolMaxIdle,
                poolMinIdle,
                poolMaxWaitMillis,
                cluster
        );
    }

    private static List<String> normalizeNodes(List<String> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>(nodes.size());
        for (String node : nodes) {
            String value = normalize(node);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return normalized;
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
        if (value instanceof String stringValue) {
            return splitList(stringValue);
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
