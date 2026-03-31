package com.cuzz.starter.bukkitspring.redis.api;

/**
 * Redis deployment mode.
 */
public enum RedisMode {
    STANDALONE,
    CLUSTER;

    public static RedisMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return STANDALONE;
        }
        String normalized = value.trim().toLowerCase();
        if ("cluster".equals(normalized)) {
            return CLUSTER;
        }
        return STANDALONE;
    }
}
