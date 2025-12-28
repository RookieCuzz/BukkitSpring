package com.monstercontroller.bukkitspring.redis;

import com.monstercontroller.bukkitspring.api.redis.RedisService;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class DefaultRedisService implements RedisService, AutoCloseable {
    private final RedisSettings settings;
    private final Logger logger;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object clientLock = new Object();
    private final Object executorLock = new Object();
    private volatile JedisPooled client;
    private volatile ExecutorService executor;

    public DefaultRedisService(RedisSettings settings, Logger logger) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = logger;
    }

    @Override
    public boolean isEnabled() {
        return settings.enabled;
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
    public JedisPooled client() {
        ensureEnabled();
        JedisPooled current = client;
        if (current != null) {
            return current;
        }
        synchronized (clientLock) {
            if (client == null) {
                client = createClient();
            }
            return client;
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        JedisPooled current = client;
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

    private JedisPooled createClient() {
        HostAndPort hostAndPort = new HostAndPort(settings.host, settings.port);
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(settings.connectionTimeoutMillis)
                .socketTimeoutMillis(settings.socketTimeoutMillis)
                .database(settings.database)
                .ssl(settings.ssl)
                .clientName(settings.clientName);
        if (!settings.user.isBlank()) {
            builder.user(settings.user);
        }
        if (!settings.password.isBlank()) {
            builder.password(settings.password);
        }
        JedisClientConfig clientConfig = builder.build();
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(settings.poolMaxTotal);
        poolConfig.setMaxIdle(settings.poolMaxIdle);
        poolConfig.setMinIdle(settings.poolMinIdle);
        poolConfig.setMaxWaitMillis(settings.poolMaxWaitMillis);
        return new JedisPooled(poolConfig, hostAndPort, clientConfig);
    }
}
