package com.cuzz.bukkitspring.platform.velocity.logging;

import com.cuzz.bukkitspring.spi.logging.LogHandlerBinder;
import com.cuzz.bukkitspring.spi.logging.LogTarget;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.spi.StandardLevel;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public final class VelocityLogHandlerBinder implements LogHandlerBinder {
    private static final String VELOCITY_PROXY_CLASS = "com.velocitypowered.api.proxy.ProxyServer";
    private final Map<Handler, String> appenderNames = new ConcurrentHashMap<>();

    @Override
    public void attach(Handler handler, LogTarget target) {
        if (handler == null || appenderNames.containsKey(handler)) {
            return;
        }
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        String name = "bukkitspring-loki-" + System.identityHashCode(handler);
        JulBridgeAppender appender = new JulBridgeAppender(name, handler);
        appender.start();
        configuration.addAppender(appender);
        Logger rootLogger = context.getRootLogger();
        rootLogger.addAppender(appender);
        context.updateLoggers();
        appenderNames.put(handler, name);
    }

    @Override
    public void detach(Handler handler) {
        if (handler == null) {
            return;
        }
        String name = appenderNames.remove(handler);
        if (name == null) {
            return;
        }
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Logger rootLogger = context.getRootLogger();
        Configuration configuration = context.getConfiguration();
        Appender appender = configuration.getAppender(name);
        if (appender != null) {
            rootLogger.removeAppender(appender);
            appender.stop();
            configuration.getAppenders().remove(name);
        }
        context.updateLoggers();
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName(VELOCITY_PROXY_CLASS);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    @Override
    public int order() {
        return 100;
    }

    private static final class JulBridgeAppender extends AbstractAppender {
        private final Handler handler;

        private JulBridgeAppender(String name, Handler handler) {
            super(name, null, null, true, null);
            this.handler = handler;
        }

        @Override
        public void append(LogEvent event) {
            if (handler == null || event == null) {
                return;
            }
            LogRecord record = new LogRecord(toJulLevel(event.getLevel()), formatMessage(event));
            record.setLoggerName(event.getLoggerName());
            record.setMillis(event.getTimeMillis());
            if (event.getThrown() != null) {
                record.setThrown(event.getThrown());
            }
            if (handler.isLoggable(record)) {
                handler.publish(record);
            }
        }

        private static java.util.logging.Level toJulLevel(Level level) {
            if (level == null) {
                return java.util.logging.Level.INFO;
            }
            StandardLevel standardLevel = level.getStandardLevel();
            return switch (standardLevel) {
                case TRACE -> java.util.logging.Level.FINER;
                case DEBUG -> java.util.logging.Level.FINE;
                case INFO -> java.util.logging.Level.INFO;
                case WARN -> java.util.logging.Level.WARNING;
                case ERROR, FATAL -> java.util.logging.Level.SEVERE;
                case OFF -> java.util.logging.Level.OFF;
                case ALL -> java.util.logging.Level.ALL;
            };
        }

        private static String formatMessage(LogEvent event) {
            Message message = event.getMessage();
            return message == null ? "" : message.getFormattedMessage();
        }
    }
}
