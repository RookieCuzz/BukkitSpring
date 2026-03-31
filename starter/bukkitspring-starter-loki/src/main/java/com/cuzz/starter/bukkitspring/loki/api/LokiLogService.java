package com.cuzz.starter.bukkitspring.loki.api;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public interface LokiLogService {
    boolean isEnabled();

    void log(Level level, String message, Map<String, String> labels);

    void log(Level level, String message, Throwable throwable, Map<String, String> labels);

    default void log(Level level, String message) {
        log(level, message, Collections.emptyMap());
    }

    default void log(Level level, String message, Throwable throwable) {
        log(level, message, throwable, Collections.emptyMap());
    }

    default CompletableFuture<Boolean> logWithAck(Level level, String message, Map<String, String> labels) {
        log(level, message, labels);
        return CompletableFuture.completedFuture(true);
    }
}
