package com.cuzz.starter.bukkitspring.loki;

import com.cuzz.bukkitspring.api.StarterRegistry;
import com.cuzz.starter.bukkitspring.loki.config.LokiSettings;
import com.cuzz.starter.bukkitspring.loki.internal.LokiLogHandler;
import com.cuzz.bukkitspring.spi.logging.LogHandlerBinder;
import com.cuzz.bukkitspring.spi.logging.LogTarget;
import com.cuzz.bukkitspring.spi.starter.StarterContext;
import com.cuzz.bukkitspring.spi.starter.StarterLifecycle;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Loki 日志 starter 入口类
 * 用于自动配置和启用 Loki 日志功能
 */
public final class LokiStarter implements StarterLifecycle {
    
    private static LokiLogHandler lokiHandler;
    private static LogHandlerBinder logHandlerBinder;
    
    static {
        // 注册到 StarterRegistry，确保在主插件加载时被发现
        StarterRegistry.registerConfiguration(LokiStarter.class);
        StarterRegistry.registerScanPackage("com.cuzz.starter.bukkitspring.loki");
    }
    
    /**
     * 初始化 Loki 日志功能
     * 
     * @param plugin Bukkit 插件实例
     * @param config 配置文件
     */
    @Override
    public void initialize(StarterContext context) {
        if (context == null || context.getConfig() == null) {
            return;
        }
        LokiSettings settings = LokiSettings.fromConfig(context.getConfig());
        
        if (settings.enabled) {
            if (settings.url == null || settings.url.isBlank()) {
                context.getLogger().warning("Loki is enabled but loki.url is empty.");
            } else {
                lokiHandler = new LokiLogHandler(settings);
                logHandlerBinder = findBinder(context.getClassLoader(), context.getLogger());
                if (logHandlerBinder == null) {
                    context.getLogger().warning("[Loki] No LogHandlerBinder found. Loki handler will not be attached.");
                } else {
                    if (settings.useServerLogger) {
                        logHandlerBinder.attach(lokiHandler, LogTarget.SERVER);
                    }
                    if (settings.useRootLogger) {
                        logHandlerBinder.attach(lokiHandler, LogTarget.ROOT);
                    }
                }
            }
        }
    }
    
    /**
     * 清理 Loki 日志功能
     */
    @Override
    public void cleanup() {
        if (logHandlerBinder != null && lokiHandler != null) {
            logHandlerBinder.detach(lokiHandler);
        }

        if (lokiHandler != null) {
            lokiHandler.close();
            lokiHandler = null;
        }
        logHandlerBinder = null;
    }

    private static LogHandlerBinder findBinder(ClassLoader classLoader, Logger logger) {
        try {
            ServiceLoader<LogHandlerBinder> loader = ServiceLoader.load(LogHandlerBinder.class, classLoader);
            LogHandlerBinder selected = null;
            int selectedOrder = Integer.MIN_VALUE;
            for (LogHandlerBinder binder : loader) {
                if (binder == null || !binder.isAvailable()) {
                    continue;
                }
                int order = binder.order();
                if (selected == null || order > selectedOrder) {
                    selected = binder;
                    selectedOrder = order;
                }
            }
            return selected;
        } catch (ServiceConfigurationError error) {
            if (logger != null) {
                logger.warning("[Loki] Failed to load LogHandlerBinder: " + error.getMessage());
            }
            return null;
        }
    }
    
    /**
     * 获取当前的 Loki 处理器实例（用于测试或其他用途）
     * 
     * @return LokiLogHandler 实例，如果未启用则返回 null
     */
    public static LokiLogHandler getLokiHandler() {
        return lokiHandler;
    }
}
