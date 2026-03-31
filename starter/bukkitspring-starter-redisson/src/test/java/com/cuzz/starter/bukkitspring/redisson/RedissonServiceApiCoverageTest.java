package com.cuzz.starter.bukkitspring.redisson;

import com.cuzz.starter.bukkitspring.redisson.api.RedissonService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RedissonServiceApiCoverageTest {

    @Test
    public void shouldExposeDistributedObjectApis() {
        Set<String> methods = Set.of(RedissonService.class.getMethods()).stream()
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(methods.contains("getBucket"));
        assertTrue(methods.contains("getMap"));
        assertTrue(methods.contains("getMapCache"));
        assertTrue(methods.contains("getSet"));
        assertTrue(methods.contains("getList"));
        assertTrue(methods.contains("getQueue"));
        assertTrue(methods.contains("getDelayedQueue"));
        assertTrue(methods.contains("destroyDelayedQueue"));
        assertTrue(methods.contains("getBlockingQueue"));
        assertTrue(methods.contains("getScoredSortedSet"));
        assertTrue(methods.contains("getAtomicLong"));
        assertTrue(methods.contains("getLock"));
        assertTrue(methods.contains("withLock"));
        assertTrue(methods.contains("tryWithLock"));
        assertTrue(methods.contains("getRateLimiter"));
        assertTrue(methods.contains("getLocalCachedMap"));
        assertTrue(methods.contains("createBatch"));
        assertTrue(methods.contains("getExecutorService"));
        assertTrue(methods.contains("getScript"));
        assertTrue(methods.contains("getIdGenerator"));
        assertTrue(methods.contains("getBloomFilter"));
        assertTrue(methods.contains("getRemoteService"));
        assertTrue(methods.contains("getSearch"));
        assertTrue(methods.contains("createTransaction"));
        assertTrue(methods.contains("withTransaction"));
        assertTrue(methods.contains("getReadWriteLock"));
        assertTrue(methods.contains("getSemaphore"));
        assertTrue(methods.contains("getCountDownLatch"));
        assertTrue(methods.contains("getTopic"));
        assertTrue(methods.contains("getReliableTopic"));
        assertTrue(methods.contains("getStream"));
        assertTrue(methods.contains("runAsync"));
        assertTrue(methods.contains("supplyAsync"));
    }
}
