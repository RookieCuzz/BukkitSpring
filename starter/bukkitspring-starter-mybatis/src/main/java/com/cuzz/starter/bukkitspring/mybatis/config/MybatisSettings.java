package com.cuzz.starter.bukkitspring.mybatis.config;

import com.cuzz.bukkitspring.spi.config.ConfigSection;
import com.cuzz.bukkitspring.spi.config.ConfigView;

public final class MybatisSettings {
    public final boolean enabled;
    public final boolean debug;
    public final boolean autoCommit;
    public final String jdbcUrl;
    public final String username;
    public final String password;
    public final String driverClassName;
    public final int poolMaxSize;
    public final int poolMinIdle;
    public final long connectionTimeoutMs;
    public final long poolMaxLifetimeMs;
    public final long poolIdleTimeoutMs;
    public final long poolKeepaliveTimeMs;
    public final long poolValidationTimeoutMs;
    public final long poolLeakDetectionThresholdMs;
    public final String logImpl;
    public final long slowSqlThresholdMs;

    private MybatisSettings(boolean enabled,
                            boolean debug,
                            boolean autoCommit,
                            String jdbcUrl,
                            String username,
                            String password,
                            String driverClassName,
                            int poolMaxSize,
                            int poolMinIdle,
                            long connectionTimeoutMs,
                            long poolMaxLifetimeMs,
                            long poolIdleTimeoutMs,
                            long poolKeepaliveTimeMs,
                            long poolValidationTimeoutMs,
                            long poolLeakDetectionThresholdMs,
                            String logImpl,
                            long slowSqlThresholdMs) {
        this.enabled = enabled;
        this.debug = debug;
        this.autoCommit = autoCommit;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.poolMaxSize = poolMaxSize;
        this.poolMinIdle = poolMinIdle;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.poolMaxLifetimeMs = poolMaxLifetimeMs;
        this.poolIdleTimeoutMs = poolIdleTimeoutMs;
        this.poolKeepaliveTimeMs = poolKeepaliveTimeMs;
        this.poolValidationTimeoutMs = poolValidationTimeoutMs;
        this.poolLeakDetectionThresholdMs = poolLeakDetectionThresholdMs;
        this.logImpl = logImpl;
        this.slowSqlThresholdMs = slowSqlThresholdMs;
    }

    public static MybatisSettings fromConfig(ConfigView config) {
        ConfigView safeConfig = config == null ? EmptyConfigView.INSTANCE : config;
        boolean enabled = safeConfig.getBoolean("mybatis.enabled", false);
        boolean debug = safeConfig.getBoolean("mybatis.debug", false);
        boolean autoCommit = safeConfig.getBoolean("mybatis.auto-commit", true);

        String jdbcUrl = normalize(safeConfig.getString("mybatis.jdbc-url", ""));
        String username = normalize(safeConfig.getString("mybatis.username", ""));
        String password = normalize(safeConfig.getString("mybatis.password", ""));
        String driverClassName = normalizeOrDefault(
                safeConfig.getString("mybatis.driver", "com.mysql.cj.jdbc.Driver"),
                "com.mysql.cj.jdbc.Driver"
        );

        int poolMaxSize = clampInt(safeConfig.getInt("mybatis.pool.max-size", 10), 1, 200);
        int poolMinIdle = clampInt(safeConfig.getInt("mybatis.pool.min-idle", 5), 0, 100);
        long connectionTimeoutMs = clampLong(
                safeConfig.getLong("mybatis.pool.connection-timeout-ms",
                        safeConfig.getLong("mybatis.connection-timeout-ms", 30000)),
                1000,
                120000
        );
        long poolMaxLifetimeMs = clampLongOptional(
                safeConfig.getLong("mybatis.pool.max-lifetime-ms", -1),
                30000,
                3600000
        );
        long poolIdleTimeoutMs = clampLongOptional(
                safeConfig.getLong("mybatis.pool.idle-timeout-ms", -1),
                10000,
                1200000
        );
        long poolKeepaliveTimeMs = clampLongOptional(
                safeConfig.getLong("mybatis.pool.keepalive-time-ms", -1),
                30000,
                1200000
        );
        long poolValidationTimeoutMs = clampLongOptional(
                safeConfig.getLong("mybatis.pool.validation-timeout-ms", -1),
                250,
                30000
        );
        long poolLeakDetectionThresholdMs = clampLongOptional(
                safeConfig.getLong("mybatis.pool.leak-detection-threshold-ms", -1),
                2000,
                600000
        );

        String logImpl = normalizeOrDefault(
                safeConfig.getString("mybatis.log-impl", "JDK_LOGGING"),
                "JDK_LOGGING"
        );
        long slowSqlThresholdMs = clampLong(
                safeConfig.getLong("mybatis.slow-sql-threshold-ms", 0),
                0,
                600000
        );

        return new MybatisSettings(
                enabled,
                debug,
                autoCommit,
                jdbcUrl,
                username,
                password,
                driverClassName,
                poolMaxSize,
                poolMinIdle,
                connectionTimeoutMs,
                poolMaxLifetimeMs,
                poolIdleTimeoutMs,
                poolKeepaliveTimeMs,
                poolValidationTimeoutMs,
                poolLeakDetectionThresholdMs,
                logImpl,
                slowSqlThresholdMs
        );
    }

    public boolean hasRequiredJdbc() {
        return !jdbcUrl.isEmpty();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String normalizeOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? fallback : normalized;
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

    private static long clampLongOptional(long value, long min, long max) {
        if (value < 0) {
            return value;
        }
        return clampLong(value, min, max);
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
