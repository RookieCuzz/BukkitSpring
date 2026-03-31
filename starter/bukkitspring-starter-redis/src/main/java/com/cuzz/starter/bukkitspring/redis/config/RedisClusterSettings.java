package com.cuzz.starter.bukkitspring.redis.config;

import java.util.Collections;
import java.util.List;

/**
 * Redis cluster settings.
 */
public final class RedisClusterSettings {
    public final boolean enabled;
    public final List<String> nodes;
    public final int maxRedirects;
    public final long topologyRefreshMillis;

    public RedisClusterSettings(boolean enabled,
                                List<String> nodes,
                                int maxRedirects,
                                long topologyRefreshMillis) {
        this.enabled = enabled;
        this.nodes = nodes == null ? Collections.emptyList() : Collections.unmodifiableList(nodes);
        this.maxRedirects = maxRedirects;
        this.topologyRefreshMillis = topologyRefreshMillis;
    }

    public boolean hasNodes() {
        return !nodes.isEmpty();
    }
}
