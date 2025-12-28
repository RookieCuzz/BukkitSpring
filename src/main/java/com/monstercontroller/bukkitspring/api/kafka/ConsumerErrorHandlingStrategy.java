package com.monstercontroller.bukkitspring.api.kafka;

public enum ConsumerErrorHandlingStrategy {
    SKIP,
    RETRY,
    DLQ,
    STOP;

    public static ConsumerErrorHandlingStrategy fromString(String value) {
        if (value == null) {
            return SKIP;
        }
        String normalized = value.trim().toUpperCase();
        for (ConsumerErrorHandlingStrategy strategy : values()) {
            if (strategy.name().equals(normalized)) {
                return strategy;
            }
        }
        return SKIP;
    }
}
