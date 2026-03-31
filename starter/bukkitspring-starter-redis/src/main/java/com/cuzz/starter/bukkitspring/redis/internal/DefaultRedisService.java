package com.cuzz.starter.bukkitspring.redis.internal;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.bukkitspring.api.annotation.PreDestroy;
import com.cuzz.starter.bukkitspring.redis.api.RedisMode;
import com.cuzz.starter.bukkitspring.redis.api.RedisService;
import com.cuzz.starter.bukkitspring.redis.config.RedisSettings;
import redis.clients.jedis.UnifiedJedis;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Component
public final class DefaultRedisService implements RedisService {
    private final RedisSettings settings;
    private final Logger logger;
    private final RedisClientFactory clientFactory;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object clientLock = new Object();
    private final Object executorLock = new Object();
    private volatile UnifiedJedis client;
    private volatile ExecutorService executor;

    @Autowired
    public DefaultRedisService(RedisSettings settings, Logger logger) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = logger;
        this.clientFactory = new RedisClientFactory();
    }

    @PostConstruct
    public void registerGlobalBean() {
        if (!settings.enabled) {
            logInfo("[Redis] Disabled, skip global bean registration.");
            return;
        }
        registerGlobalBeanInternal();
    }

    @PreDestroy
    public void preDestroy() {
        close();
    }

    @Override
    public boolean isEnabled() {
        return settings.enabled && !closed.get();
    }

    @Override
    public RedisMode mode() {
        return settings.mode;
    }

    @Override
    public ExecutorService executor() {
        if (closed.get()) {
            throw new IllegalStateException("Redis service is closed.");
        }
        ExecutorService current = executor;
        if (current != null) {
            return current;
        }
        synchronized (executorLock) {
            if (executor == null) {
                executor = RedisExecutors.create(settings.useVirtualThreads, logger);
            }
            return executor;
        }
    }

    @Override
    public UnifiedJedis client() {
        ensureEnabled();
        UnifiedJedis current = client;
        if (current != null) {
            return current;
        }
        synchronized (clientLock) {
            if (client == null) {
                client = clientFactory.create(settings, logger);
            }
            return client;
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        UnifiedJedis current = client;
        if (current != null) {
            current.close();
        }
        ExecutorService currentExecutor = executor;
        if (currentExecutor != null) {
            currentExecutor.shutdown();
        }
    }

    private void ensureEnabled() {
        if (!settings.enabled) {
            throw new IllegalStateException("Redis is disabled (redis.enabled=false).");
        }
        if (closed.get()) {
            throw new IllegalStateException("Redis service is closed.");
        }
    }

    private void registerGlobalBeanInternal() {
        try {
            Class<?> bukkitSpringClass = Class.forName("com.cuzz.bukkitspring.BukkitSpring");
            java.lang.reflect.Method registerMethod = bukkitSpringClass
                    .getMethod("registerGlobalBean", Class.class, Object.class);
            registerMethod.invoke(null, RedisService.class, this);
            logInfo("[Redis] RedisService registered as global bean");
        } catch (Exception e) {
            logWarning("[Redis] Failed to register global bean: " + e.getMessage());
        }
    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    private void logWarning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }
}
