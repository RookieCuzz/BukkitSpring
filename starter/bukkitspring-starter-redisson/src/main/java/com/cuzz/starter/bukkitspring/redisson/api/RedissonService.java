package com.cuzz.starter.bukkitspring.redisson.api;

import com.cuzz.starter.bukkitspring.redisson.config.RedissonSettings;
import org.redisson.api.BatchOptions;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBatch;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RBucket;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RIdGenerator;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RQueue;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RRemoteService;
import org.redisson.api.RReliableTopic;
import org.redisson.api.RScheduledExecutorService;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSearch;
import org.redisson.api.RScript;
import org.redisson.api.RSemaphore;
import org.redisson.api.RSet;
import org.redisson.api.RStream;
import org.redisson.api.RTransaction;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.TransactionOptions;
import org.redisson.api.options.ExecutorOptions;
import org.redisson.api.options.LocalCachedMapOptions;
import org.redisson.api.options.OptionalOptions;
import org.redisson.api.options.PlainOptions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Redisson facade service.
 */
public interface RedissonService extends AutoCloseable {
    boolean isEnabled();

    RedissonSettings settings();

    ExecutorService executor();

    RedissonClient client();

    default CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor());
    }

    default <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor());
    }

    default <V> RBucket<V> getBucket(String name) {
        return client().getBucket(name);
    }

    default <K, V> RMap<K, V> getMap(String name) {
        return client().getMap(name);
    }

    default <K, V> RMapCache<K, V> getMapCache(String name) {
        return client().getMapCache(name);
    }

    default <V> RSet<V> getSet(String name) {
        return client().getSet(name);
    }

    default <V> RList<V> getList(String name) {
        return client().getList(name);
    }

    default <V> RQueue<V> getQueue(String name) {
        return client().getQueue(name);
    }

    default <V> RDelayedQueue<V> getDelayedQueue(String queueName) {
        return client().getDelayedQueue(getQueue(queueName));
    }

    /**
     * Destroy delayed queue metadata for the given queue.
     *
     * <p>Call this when you permanently remove the delayed-queue feature for this key.
     */
    default void destroyDelayedQueue(String queueName) {
        getDelayedQueue(queueName).destroy();
    }

    default <V> RBlockingQueue<V> getBlockingQueue(String name) {
        return client().getBlockingQueue(name);
    }

    default <V> RScoredSortedSet<V> getScoredSortedSet(String name) {
        return client().getScoredSortedSet(name);
    }

    default RAtomicLong getAtomicLong(String name) {
        return client().getAtomicLong(name);
    }

    default RLock getLock(String name) {
        return client().getLock(name);
    }

    /**
     * Execute logic under a distributed lock, always releasing the lock in finally.
     */
    default void withLock(String lockName, Runnable action) {
        if (action == null) {
            return;
        }
        RLock lock = getLock(lockName);
        lock.lock();
        try {
            action.run();
        } finally {
            unlockQuietly(lock);
        }
    }

    /**
     * Execute logic under a distributed lock and return result.
     */
    default <T> T withLock(String lockName, Supplier<T> action) {
        if (action == null) {
            return null;
        }
        RLock lock = getLock(lockName);
        lock.lock();
        try {
            return action.get();
        } finally {
            unlockQuietly(lock);
        }
    }

    /**
     * Try lock and run action only when lock acquired.
     */
    default boolean tryWithLock(String lockName, long waitMillis, long leaseMillis, Runnable action) {
        if (action == null) {
            return false;
        }
        RLock lock = getLock(lockName);
        boolean acquired = tryAcquire(lock, waitMillis, leaseMillis);
        if (!acquired) {
            return false;
        }
        try {
            action.run();
            return true;
        } finally {
            unlockQuietly(lock);
        }
    }

    /**
     * Try lock and return fallback when lock is not acquired.
     */
    default <T> T tryWithLock(String lockName, long waitMillis, long leaseMillis, Supplier<T> action, T onFailure) {
        if (action == null) {
            return onFailure;
        }
        RLock lock = getLock(lockName);
        boolean acquired = tryAcquire(lock, waitMillis, leaseMillis);
        if (!acquired) {
            return onFailure;
        }
        try {
            return action.get();
        } finally {
            unlockQuietly(lock);
        }
    }

    default RRateLimiter getRateLimiter(String name) {
        return client().getRateLimiter(name);
    }

    default <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String name) {
        return client().getLocalCachedMap(LocalCachedMapOptions.<K, V>name(name));
    }

    default <K, V> RLocalCachedMap<K, V> getLocalCachedMap(LocalCachedMapOptions<K, V> options) {
        if (options == null) {
            throw new IllegalArgumentException("LocalCachedMapOptions must not be null.");
        }
        return client().getLocalCachedMap(options);
    }

    /**
     * Compatibility bridge for old Redisson API.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    default <K, V> RLocalCachedMap<K, V> getLocalCachedMap(String name, org.redisson.api.LocalCachedMapOptions<K, V> options) {
        if (options == null) {
            return getLocalCachedMap(name);
        }
        return client().getLocalCachedMap(name, options);
    }

    default RBatch createBatch() {
        return client().createBatch();
    }

    default RBatch createBatch(BatchOptions options) {
        if (options == null) {
            return createBatch();
        }
        return client().createBatch(options);
    }

    default RScheduledExecutorService getExecutorService(String name) {
        return client().getExecutorService(name);
    }

    default RScheduledExecutorService getExecutorService(ExecutorOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("ExecutorOptions must not be null.");
        }
        return client().getExecutorService(options);
    }

    default RScript getScript() {
        return client().getScript();
    }

    default RIdGenerator getIdGenerator(String name) {
        return client().getIdGenerator(name);
    }

    default <V> RBloomFilter<V> getBloomFilter(String name) {
        return client().getBloomFilter(name);
    }

    default RRemoteService getRemoteService(String name) {
        return client().getRemoteService(name);
    }

    default RRemoteService getRemoteService(PlainOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("PlainOptions must not be null.");
        }
        return client().getRemoteService(options);
    }

    default RSearch getSearch() {
        return client().getSearch();
    }

    default RSearch getSearch(OptionalOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("OptionalOptions must not be null.");
        }
        return client().getSearch(options);
    }

    default RTransaction createTransaction() {
        return client().createTransaction(TransactionOptions.defaults());
    }

    default RTransaction createTransaction(TransactionOptions options) {
        if (options == null) {
            return createTransaction();
        }
        return client().createTransaction(options);
    }

    default <T> T withTransaction(Function<RTransaction, T> action) {
        return withTransaction(TransactionOptions.defaults(), action);
    }

    default <T> T withTransaction(TransactionOptions options, Function<RTransaction, T> action) {
        if (action == null) {
            return null;
        }
        RTransaction transaction = createTransaction(options);
        boolean committed = false;
        try {
            T result = action.apply(transaction);
            transaction.commit();
            committed = true;
            return result;
        } finally {
            if (!committed) {
                rollbackQuietly(transaction);
            }
        }
    }

    default RReadWriteLock getReadWriteLock(String name) {
        return client().getReadWriteLock(name);
    }

    default RSemaphore getSemaphore(String name) {
        return client().getSemaphore(name);
    }

    default RCountDownLatch getCountDownLatch(String name) {
        return client().getCountDownLatch(name);
    }

    default RTopic getTopic(String name) {
        return client().getTopic(name);
    }

    default RReliableTopic getReliableTopic(String name) {
        return client().getReliableTopic(name);
    }

    default <K, V> RStream<K, V> getStream(String name) {
        return client().getStream(name);
    }

    private static boolean tryAcquire(RLock lock, long waitMillis, long leaseMillis) {
        long safeWait = Math.max(0L, waitMillis);
        long safeLease = leaseMillis;
        try {
            if (safeLease > 0L) {
                return lock.tryLock(safeWait, safeLease, TimeUnit.MILLISECONDS);
            }
            return lock.tryLock(safeWait, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void unlockQuietly(RLock lock) {
        if (lock == null) {
            return;
        }
        try {
            lock.unlock();
        } catch (Exception ignored) {
        }
    }

    private static void rollbackQuietly(RTransaction transaction) {
        if (transaction == null) {
            return;
        }
        try {
            transaction.rollback();
        } catch (Exception ignored) {
        }
    }

    @Override
    void close();
}
