package com.monstercontroller.bukkitspring.redis;

import org.bukkit.configuration.file.FileConfiguration;

public final class RedisSettings {
    public final boolean enabled;
    public final boolean useVirtualThreads;
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

    private RedisSettings(boolean enabled,
                          boolean useVirtualThreads,
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
                          long poolMaxWaitMillis) {
        this.enabled = enabled;
        this.useVirtualThreads = useVirtualThreads;
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
    }

    public static RedisSettings fromConfig(FileConfiguration config) {
        boolean enabled = config.getBoolean("redis.enabled", false);
        boolean useVirtualThreads = config.getBoolean("redis.virtual-threads", true);
        String host = normalize(config.getString("redis.host", "localhost"));
        int port = clampInt(config.getInt("redis.port", 6379), 1, 65535);
        String user = normalize(config.getString("redis.user", ""));
        String password = normalize(config.getString("redis.password", ""));
        int database = clampInt(config.getInt("redis.database", 0), 0, 15);
        boolean ssl = config.getBoolean("redis.ssl", false);
        int connectionTimeoutMillis = clampInt(config.getInt("redis.timeouts.connect-ms", 2000), 1, 60000);
        int socketTimeoutMillis = clampInt(config.getInt("redis.timeouts.socket-ms", 2000), 1, 60000);
        String clientName = normalize(config.getString("redis.client-name", "bukkitspring"));
        int poolMaxTotal = clampInt(config.getInt("redis.pool.max-total", 16), 1, 512);
        int poolMaxIdle = clampInt(config.getInt("redis.pool.max-idle", 16), 0, 512);
        int poolMinIdle = clampInt(config.getInt("redis.pool.min-idle", 0), 0, 512);
        long poolMaxWaitMillis = clampLong(config.getLong("redis.pool.max-wait-ms", 3000), 1, 60000);

        return new RedisSettings(
                enabled,
                useVirtualThreads,
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
                poolMaxWaitMillis
        );
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
