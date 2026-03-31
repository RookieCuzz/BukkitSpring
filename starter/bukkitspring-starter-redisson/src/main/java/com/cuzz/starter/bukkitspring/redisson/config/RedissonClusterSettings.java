package com.cuzz.starter.bukkitspring.redisson.config;

import java.util.List;

/**
 * Cluster-specific Redisson settings.
 */
public final class RedissonClusterSettings {
    public final boolean enabled;
    public final List<String> nodes;
    public final int scanIntervalMillis;

    public RedissonClusterSettings(boolean enabled, List<String> nodes, int scanIntervalMillis) {
        this.enabled = enabled;
        this.nodes = nodes == null ? List.of() : List.copyOf(nodes);
        this.scanIntervalMillis = scanIntervalMillis;
    }
}
