package com.cuzz.starter.bukkitspring.caffeine.config;

import com.cuzz.bukkitspring.spi.config.ConfigSection;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.starter.bukkitspring.caffeine.api.CaffeineCacheSpec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Caffeine starter settings.
 */
public final class CaffeineSettings {
    private static final int MAX_INITIAL_CAPACITY = 1_000_000;
    private static final long MAX_MAXIMUM_SIZE = 100_000_000L;
    private static final long MAX_DURATION_MILLIS = 31_536_000_000L;

    public final boolean enabled;
    public final boolean useVirtualThreads;
    public final String defaultCacheName;
    public final CaffeineCacheSpec defaultCacheSpec;
    public final Map<String, CaffeineCacheSpec> namedCacheSpecs;

    private CaffeineSettings(boolean enabled,
                             boolean useVirtualThreads,
                             String defaultCacheName,
                             CaffeineCacheSpec defaultCacheSpec,
                             Map<String, CaffeineCacheSpec> namedCacheSpecs) {
        this.enabled = enabled;
        this.useVirtualThreads = useVirtualThreads;
        this.defaultCacheName = defaultCacheName;
        this.defaultCacheSpec = defaultCacheSpec;
        this.namedCacheSpecs = namedCacheSpecs;
    }

    public static CaffeineSettings fromConfig(ConfigView config) {
        ConfigView safeConfig = config == null ? EmptyConfigView.INSTANCE : config;
        boolean enabled = safeConfig.getBoolean("caffeine.enabled", false);
        boolean useVirtualThreads = safeConfig.getBoolean("caffeine.virtual-threads", true);
        String defaultCacheName = normalizeOrDefault(safeConfig.getString("caffeine.default-cache-name", "default"), "default");

        CaffeineCacheSpec defaultSpec = readCacheSpec(safeConfig, "caffeine.default", CaffeineCacheSpec.defaults());
        Map<String, CaffeineCacheSpec> namedSpecs = readNamedCacheSpecs(safeConfig, defaultSpec);

        return new CaffeineSettings(enabled, useVirtualThreads, defaultCacheName, defaultSpec, namedSpecs);
    }

    public CaffeineCacheSpec specFor(String cacheName) {
        String normalized = normalize(cacheName);
        if (normalized.isEmpty()) {
            return defaultCacheSpec;
        }
        CaffeineCacheSpec override = namedCacheSpecs.get(normalized);
        return override == null ? defaultCacheSpec : override;
    }

    private static Map<String, CaffeineCacheSpec> readNamedCacheSpecs(ConfigView config, CaffeineCacheSpec defaultSpec) {
        ConfigSection cachesSection = config.getSection("caffeine.caches");
        if (cachesSection == null || cachesSection.keys().isEmpty()) {
            return Map.of();
        }

        Map<String, CaffeineCacheSpec> namedSpecs = new LinkedHashMap<>();
        for (String rawName : cachesSection.keys()) {
            String cacheName = normalize(rawName);
            if (cacheName.isEmpty()) {
                continue;
            }
            namedSpecs.put(cacheName, readCacheSpec(config, "caffeine.caches." + cacheName, defaultSpec));
        }
        if (namedSpecs.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(namedSpecs);
    }

    private static CaffeineCacheSpec readCacheSpec(ConfigView config, String prefix, CaffeineCacheSpec fallback) {
        int initialCapacity = clampInt(
                config.getInt(prefix + ".initial-capacity", fallback.initialCapacity),
                0,
                MAX_INITIAL_CAPACITY
        );
        long maximumSize = clampLong(
                config.getLong(prefix + ".maximum-size", fallback.maximumSize),
                0L,
                MAX_MAXIMUM_SIZE
        );
        long expireAfterWriteMillis = clampLong(
                config.getLong(prefix + ".expire-after-write-ms", fallback.expireAfterWriteMillis),
                0L,
                MAX_DURATION_MILLIS
        );
        long expireAfterAccessMillis = clampLong(
                config.getLong(prefix + ".expire-after-access-ms", fallback.expireAfterAccessMillis),
                0L,
                MAX_DURATION_MILLIS
        );
        long refreshAfterWriteMillis = clampLong(
                config.getLong(prefix + ".refresh-after-write-ms", fallback.refreshAfterWriteMillis),
                0L,
                MAX_DURATION_MILLIS
        );
        boolean weakKeys = config.getBoolean(prefix + ".weak-keys", fallback.weakKeys);
        boolean weakValues = config.getBoolean(prefix + ".weak-values", fallback.weakValues);
        boolean softValues = config.getBoolean(prefix + ".soft-values", fallback.softValues);
        boolean recordStats = config.getBoolean(prefix + ".record-stats", fallback.recordStats);

        return new CaffeineCacheSpec(
                initialCapacity,
                maximumSize,
                expireAfterWriteMillis,
                expireAfterAccessMillis,
                refreshAfterWriteMillis,
                weakKeys,
                weakValues,
                softValues,
                recordStats
        );
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
