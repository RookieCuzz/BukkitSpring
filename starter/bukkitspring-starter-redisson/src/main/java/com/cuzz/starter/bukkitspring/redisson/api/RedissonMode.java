package com.cuzz.starter.bukkitspring.redisson.api;

/**
 * Redisson deployment mode.
 */
public enum RedissonMode {
    SINGLE,
    CLUSTER;

    public static RedissonMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return SINGLE;
        }
        String normalized = value.trim().toUpperCase();
        if ("CLUSTER".equals(normalized)) {
            return CLUSTER;
        }
        return SINGLE;
    }
}
