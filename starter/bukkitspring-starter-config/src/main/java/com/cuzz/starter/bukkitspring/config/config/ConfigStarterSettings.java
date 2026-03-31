package com.cuzz.starter.bukkitspring.config.config;

import com.cuzz.bukkitspring.spi.config.ConfigSection;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.starter.bukkitspring.config.api.ConfigLoadStrategy;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Starter settings read from BukkitSpring config.yml.
 */
public final class ConfigStarterSettings {
    private static final long MAX_TIMEOUT_MILLIS = 120_000L;

    public final boolean enabled;
    public final boolean useVirtualThreads;
    public final ConfigLoadStrategy defaultStrategy;

    public final boolean cacheEnabled;
    public final long cacheTtlMillis;
    public final int cacheMaxEntries;

    public final boolean localEnabled;
    public final boolean localBootstrapFromResource;
    public final Charset localCharset;

    public final boolean remoteEnabled;
    public final String remoteProvider;
    public final String remoteBaseUrl;
    public final String remotePathTemplate;
    public final Charset remoteCharset;
    public final long remoteConnectTimeoutMillis;
    public final long remoteReadTimeoutMillis;
    public final boolean remoteFailOnHttpError;
    public final Map<String, String> remoteHeaders;

    private ConfigStarterSettings(boolean enabled,
                                  boolean useVirtualThreads,
                                  ConfigLoadStrategy defaultStrategy,
                                  boolean cacheEnabled,
                                  long cacheTtlMillis,
                                  int cacheMaxEntries,
                                  boolean localEnabled,
                                  boolean localBootstrapFromResource,
                                  Charset localCharset,
                                  boolean remoteEnabled,
                                  String remoteProvider,
                                  String remoteBaseUrl,
                                  String remotePathTemplate,
                                  Charset remoteCharset,
                                  long remoteConnectTimeoutMillis,
                                  long remoteReadTimeoutMillis,
                                  boolean remoteFailOnHttpError,
                                  Map<String, String> remoteHeaders) {
        this.enabled = enabled;
        this.useVirtualThreads = useVirtualThreads;
        this.defaultStrategy = defaultStrategy;
        this.cacheEnabled = cacheEnabled;
        this.cacheTtlMillis = cacheTtlMillis;
        this.cacheMaxEntries = cacheMaxEntries;
        this.localEnabled = localEnabled;
        this.localBootstrapFromResource = localBootstrapFromResource;
        this.localCharset = localCharset;
        this.remoteEnabled = remoteEnabled;
        this.remoteProvider = remoteProvider;
        this.remoteBaseUrl = remoteBaseUrl;
        this.remotePathTemplate = remotePathTemplate;
        this.remoteCharset = remoteCharset;
        this.remoteConnectTimeoutMillis = remoteConnectTimeoutMillis;
        this.remoteReadTimeoutMillis = remoteReadTimeoutMillis;
        this.remoteFailOnHttpError = remoteFailOnHttpError;
        this.remoteHeaders = remoteHeaders;
    }

    public static ConfigStarterSettings fromConfig(ConfigView config) {
        ConfigView safeConfig = config == null ? EmptyConfigView.INSTANCE : config;

        boolean enabled = safeConfig.getBoolean("config-starter.enabled", false);
        boolean useVirtualThreads = safeConfig.getBoolean("config-starter.virtual-threads", true);
        ConfigLoadStrategy defaultStrategy = ConfigLoadStrategy.fromString(
                safeConfig.getString("config-starter.source.strategy", "local-first"),
                ConfigLoadStrategy.LOCAL_FIRST
        );

        boolean cacheEnabled = safeConfig.getBoolean("config-starter.cache.enabled", true);
        long cacheTtlMillis = clampLong(safeConfig.getLong("config-starter.cache.ttl-ms", 5_000L), 0L, 3_600_000L);
        int cacheMaxEntries = clampInt(safeConfig.getInt("config-starter.cache.max-entries", 256), 1, 10_000);

        boolean localEnabled = safeConfig.getBoolean("config-starter.source.local.enabled", true);
        boolean localBootstrapFromResource = safeConfig.getBoolean("config-starter.source.local.bootstrap-from-resource", true);
        Charset localCharset = resolveCharset(safeConfig.getString("config-starter.source.local.encoding", "UTF-8"), StandardCharsets.UTF_8);

        boolean remoteEnabled = safeConfig.getBoolean("config-starter.source.remote.enabled", false);
        String remoteProvider = normalizeOrDefault(safeConfig.getString("config-starter.source.remote.provider", "http"), "http");
        String remoteBaseUrl = normalize(safeConfig.getString("config-starter.source.remote.base-url", ""));
        String remotePathTemplate = normalizeOrDefault(
                safeConfig.getString("config-starter.source.remote.path-template", "{name}"),
                "{name}"
        );
        Charset remoteCharset = resolveCharset(safeConfig.getString("config-starter.source.remote.encoding", "UTF-8"), StandardCharsets.UTF_8);
        long remoteConnectTimeoutMillis = clampLong(
                safeConfig.getLong("config-starter.source.remote.connect-timeout-ms", 2_000L),
                100L,
                MAX_TIMEOUT_MILLIS
        );
        long remoteReadTimeoutMillis = clampLong(
                safeConfig.getLong("config-starter.source.remote.read-timeout-ms", 3_000L),
                100L,
                MAX_TIMEOUT_MILLIS
        );
        boolean remoteFailOnHttpError = safeConfig.getBoolean("config-starter.source.remote.fail-on-http-error", false);
        Map<String, String> remoteHeaders = readHeaders(safeConfig.getSection("config-starter.source.remote.headers"));

        return new ConfigStarterSettings(
                enabled,
                useVirtualThreads,
                defaultStrategy,
                cacheEnabled,
                cacheTtlMillis,
                cacheMaxEntries,
                localEnabled,
                localBootstrapFromResource,
                localCharset,
                remoteEnabled,
                remoteProvider,
                remoteBaseUrl,
                remotePathTemplate,
                remoteCharset,
                remoteConnectTimeoutMillis,
                remoteReadTimeoutMillis,
                remoteFailOnHttpError,
                remoteHeaders
        );
    }

    private static Map<String, String> readHeaders(ConfigSection section) {
        if (section == null || section.keys().isEmpty()) {
            return Map.of();
        }
        Map<String, String> headers = new LinkedHashMap<>();
        for (String key : section.keys()) {
            if (key == null || key.isBlank()) {
                continue;
            }
            Object rawValue = section.get(key);
            if (rawValue == null) {
                continue;
            }
            headers.put(key.trim(), String.valueOf(rawValue));
        }
        if (headers.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(headers);
    }

    private static Charset resolveCharset(String raw, Charset fallback) {
        String value = normalize(raw);
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Charset.forName(value);
        } catch (Exception ignored) {
            return fallback;
        }
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
