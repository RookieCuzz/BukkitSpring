package com.cuzz.bukkitspring.platform.bc.logging;

import com.cuzz.bukkitspring.spi.logging.LogHandlerBinder;
import com.cuzz.bukkitspring.spi.logging.LogTarget;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Logger;

public final class BungeeLogHandlerBinder implements LogHandlerBinder {
    private static final String PROXY_SERVER_CLASS = "net.md_5.bungee.api.ProxyServer";
    private final Map<Handler, Set<Logger>> attachments = new ConcurrentHashMap<>();

    @Override
    public void attach(Handler handler, LogTarget target) {
        if (handler == null) {
            return;
        }
        Logger logger = resolveLogger();
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
    public boolean isAvailable() {
        try {
            Class.forName(PROXY_SERVER_CLASS);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    @Override
    public int order() {
        return 100;
    }

    private Logger resolveLogger() {
        try {
            Class<?> proxyClass = Class.forName(PROXY_SERVER_CLASS);
            Object proxy = proxyClass.getMethod("getInstance").invoke(null);
            if (proxy == null) {
                return null;
            }
            Object logger = proxyClass.getMethod("getLogger").invoke(proxy);
            return logger instanceof Logger ? (Logger) logger : null;
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private void track(Handler handler, Logger logger) {
        attachments.computeIfAbsent(handler, ignored -> ConcurrentHashMap.newKeySet()).add(logger);
    }
}
