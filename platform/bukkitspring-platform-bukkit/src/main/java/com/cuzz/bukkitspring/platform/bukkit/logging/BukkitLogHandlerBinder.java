package com.cuzz.bukkitspring.platform.bukkit.logging;

import com.cuzz.bukkitspring.spi.logging.LogHandlerBinder;
import com.cuzz.bukkitspring.spi.logging.LogTarget;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Logger;

public final class BukkitLogHandlerBinder implements LogHandlerBinder {
    private final Map<Handler, Set<Logger>> attachments = new ConcurrentHashMap<>();

    @Override
    public void attach(Handler handler, LogTarget target) {
        if (handler == null || target == null) {
            return;
        }
        Logger logger = resolveLogger(target);
        if (logger == null) {
            return;
        }
        for (Handler existing : logger.getHandlers()) {
            if (existing == handler) {
                track(handler, logger);
                return;
            }
        }
        logger.addHandler(handler);
        track(handler, logger);
    }

    @Override
    public void detach(Handler handler) {
        if (handler == null) {
            return;
        }
        Set<Logger> loggers = attachments.remove(handler);
        if (loggers == null) {
            return;
        }
        for (Logger logger : loggers) {
            logger.removeHandler(handler);
        }
    }

    @Override
    public int order() {
        return 100;
    }

    private Logger resolveLogger(LogTarget target) {
        return switch (target) {
            case SERVER -> Bukkit.getServer() == null ? null : Bukkit.getServer().getLogger();
            case ROOT -> Logger.getLogger("");
        };
    }

    private void track(Handler handler, Logger logger) {
        attachments.computeIfAbsent(handler, ignored -> ConcurrentHashMap.newKeySet()).add(logger);
    }
}
