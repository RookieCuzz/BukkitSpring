package com.cuzz.starter.bukkitspring.redisson.internal;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.bukkitspring.api.annotation.PreDestroy;
import com.cuzz.starter.bukkitspring.redisson.api.RedissonService;
import com.cuzz.starter.bukkitspring.redisson.config.RedissonSettings;
import org.redisson.api.RedissonClient;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Component
public final class DefaultRedissonService implements RedissonService {
    private final RedissonSettings settings;
    private final Logger logger;
    private final RedissonClientFactory clientFactory;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object clientLock = new Object();
    private final Object executorLock = new Object();
    private volatile RedissonClient client;
    private volatile ExecutorService executor;

    @Autowired
    public DefaultRedissonService(RedissonSettings settings, Logger logger) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = logger;
        this.clientFactory = new RedissonClientFactory();
    }

    @PostConstruct
    public void registerGlobalBean() {
        if (!settings.enabled) {
            logInfo("[Redisson] Disabled, skip global bean registration.");
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
    public RedissonSettings settings() {
        return settings;
    }

    @Override
    public ExecutorService executor() {
        if (closed.get()) {
            throw new IllegalStateException("Redisson service is closed.");
        }
        ExecutorService current = executor;
        if (current != null) {
            return current;
        }
        synchronized (executorLock) {
            if (closed.get()) {
                throw new IllegalStateException("Redisson service is closed.");
            }
            if (executor == null) {
                ExecutorService created = RedissonExecutors.create(settings.useVirtualThreads, logger);
                if (closed.get()) {
                    created.shutdown();
                    throw new IllegalStateException("Redisson service is closed.");
                }
                executor = created;
            }
            return executor;
        }
    }

    @Override
    public RedissonClient client() {
        ensureEnabled();
        RedissonClient current = client;
        if (current != null) {
            return current;
        }
        synchronized (clientLock) {
            ensureEnabled();
            if (client == null) {
                RedissonClient created = clientFactory.create(settings, logger);
                if (closed.get()) {
                    created.shutdown();
                    throw new IllegalStateException("Redisson service is closed.");
                }
                client = created;
            }
            return client;
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        RedissonClient current;
        synchronized (clientLock) {
            current = client;
            client = null;
        }
        if (current != null) {
            current.shutdown();
        }
        ExecutorService currentExecutor;
        synchronized (executorLock) {
            currentExecutor = executor;
            executor = null;
        }
        if (currentExecutor != null) {
            currentExecutor.shutdown();
        }
    }

    private void ensureEnabled() {
        if (!settings.enabled) {
            throw new IllegalStateException("Redisson is disabled (redisson.enabled=false).");
        }
        if (closed.get()) {
            throw new IllegalStateException("Redisson service is closed.");
        }
    }

    private void registerGlobalBeanInternal() {
        try {
            Class<?> bukkitSpringClass = Class.forName("com.cuzz.bukkitspring.BukkitSpring");
            java.lang.reflect.Method registerMethod = bukkitSpringClass
                    .getMethod("registerGlobalBean", Class.class, Object.class);
            registerMethod.invoke(null, RedissonService.class, this);
            logInfo("[Redisson] RedissonService registered as global bean");
        } catch (Exception e) {
            logWarning("[Redisson] Failed to register global bean: " + e.getMessage());
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
