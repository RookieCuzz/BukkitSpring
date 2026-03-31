package com.cuzz.starter.bukkitspring.config.api;

import java.util.Objects;

/**
 * Config load request.
 */
public final class ConfigQuery {
    private final String name;
    private final String pluginName;
    private final String remoteName;
    private final ConfigLoadStrategy strategy;
    private final boolean bypassCache;

    private ConfigQuery(Builder builder) {
        this.name = normalizeName(builder.name);
        this.pluginName = normalize(builder.pluginName);
        this.remoteName = normalizeOrDefault(builder.remoteName, this.name);
        this.strategy = builder.strategy;
        this.bypassCache = builder.bypassCache;
    }

    public static ConfigQuery of(String name) {
        return builder(name).build();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    /**
     * Optional caller plugin name. When provided, local file reads should be resolved
     * against that plugin's data directory.
     */
    public String pluginName() {
        return pluginName;
    }

    public String remoteName() {
        return remoteName;
    }

    /**
     * If null, the starter default strategy is used.
     */
    public ConfigLoadStrategy strategy() {
        return strategy;
    }

    public boolean bypassCache() {
        return bypassCache;
    }

    public String cacheKey(ConfigLoadStrategy effectiveStrategy) {
        ConfigLoadStrategy finalStrategy = effectiveStrategy == null ? ConfigLoadStrategy.LOCAL_FIRST : effectiveStrategy;
        return name + "|" + pluginName + "|" + remoteName + "|" + finalStrategy.name();
    }

    private static String normalizeName(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Config name cannot be blank.");
        }
        return normalized;
    }

    private static String normalizeOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public static final class Builder {
        private final String name;
        private String pluginName;
        private String remoteName;
        private ConfigLoadStrategy strategy;
        private boolean bypassCache;

        private Builder(String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        public Builder remoteName(String remoteName) {
            this.remoteName = remoteName;
            return this;
        }

        public Builder plugin(String pluginName) {
            this.pluginName = pluginName;
            return this;
        }

        public Builder strategy(ConfigLoadStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder bypassCache(boolean bypassCache) {
            this.bypassCache = bypassCache;
            return this;
        }

        public ConfigQuery build() {
            return new ConfigQuery(this);
        }
    }
}
