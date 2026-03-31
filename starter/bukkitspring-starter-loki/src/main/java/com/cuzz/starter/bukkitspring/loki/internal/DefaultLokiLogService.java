package com.cuzz.starter.bukkitspring.loki.internal;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.starter.bukkitspring.loki.LokiStarter;
import com.cuzz.starter.bukkitspring.loki.api.LokiLogService;
import com.cuzz.starter.bukkitspring.loki.config.LokiSettings;

import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

@Component
public final class DefaultLokiLogService implements LokiLogService {
    private final LokiSettings settings;
    private final Logger logger;

    @Autowired
    public DefaultLokiLogService(LokiSettings settings, Logger logger) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = logger;
    }

    @PostConstruct
    public void registerGlobalBean() {
        if (!settings.enabled) {
            if (logger != null) {
                logger.info("[Loki] Disabled, skip global bean registration.");
            }
            return;
        }
        try {
            Class<?> bukkitSpringClass = Class.forName("com.cuzz.bukkitspring.BukkitSpring");
            java.lang.reflect.Method registerMethod = bukkitSpringClass.getMethod("registerGlobalBean", Class.class, Object.class);
            registerMethod.invoke(null, LokiLogService.class, this);
            if (logger != null) {
                logger.info("[Loki] LokiLogService registered as global bean");
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.warning("[Loki] Failed to register global bean: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return settings.enabled;
    }

    @Override
    public void log(Level level, String message, Map<String, String> labels) {
        log(level, message, null, labels);
    }

    @Override
    public void log(Level level, String message, Throwable throwable, Map<String, String> labels) {
        if (!settings.enabled) {
            return;
        }
        LokiLogHandler handler = LokiStarter.getLokiHandler();
        if (handler == null) {
            return;
        }
        LogRecord record = buildRecord(level, message, throwable, labels);
        handler.enqueue(record, labels);
    }

    @Override
    public CompletableFuture<Boolean> logWithAck(Level level, String message, Map<String, String> labels) {
        if (!settings.enabled) {
            return CompletableFuture.completedFuture(false);
        }
        LokiLogHandler handler = LokiStarter.getLokiHandler();
        if (handler == null) {
            return CompletableFuture.completedFuture(false);
        }
        LogRecord record = buildRecord(level, message, null, labels);
        return handler.enqueueWithAck(record, labels);
    }

    private LogRecord buildRecord(Level level, String message, Throwable throwable, Map<String, String> labels) {
        Level resolvedLevel = level == null ? Level.INFO : level;
        String resolvedMessage = message == null ? "" : message;
        LogRecord record = new LogRecord(resolvedLevel, resolvedMessage);
        record.setMillis(System.currentTimeMillis());
        record.setThrown(throwable);
        if (labels != null) {
            String loggerLabel = labels.get("logger");
            if (loggerLabel != null && !loggerLabel.isBlank()) {
                record.setLoggerName(loggerLabel);
            }
        }
        return record;
    }
}
