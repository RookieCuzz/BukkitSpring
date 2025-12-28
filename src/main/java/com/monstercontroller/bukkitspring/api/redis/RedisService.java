package com.monstercontroller.bukkitspring.api.redis;

import redis.clients.jedis.JedisPooled;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public interface RedisService {
    boolean isEnabled();

    ExecutorService executor();

    JedisPooled client();

    default CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor());
    }

    default <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor());
    }
}
