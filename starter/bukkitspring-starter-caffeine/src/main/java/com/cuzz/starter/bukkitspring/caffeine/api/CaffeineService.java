package com.cuzz.starter.bukkitspring.caffeine.api;

import com.cuzz.starter.bukkitspring.caffeine.config.CaffeineSettings;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Caffeine cache facade service.
 *
 * <p>Provides wrappers around Caffeine's core interfaces:
 * {@link Cache}, {@link AsyncCache}, {@link LoadingCache},
 * {@link AsyncLoadingCache}, and {@link Policy}.
 */
public interface CaffeineService extends AutoCloseable {
    boolean isEnabled();

    CaffeineSettings settings();

    ExecutorService executor();

    /**
     * Create a preconfigured builder from cache settings.
     */
    Caffeine<Object, Object> newBuilder(String cacheName);

    Cache<Object, Object> defaultCache();

    Cache<Object, Object> getCache(String cacheName);

    AsyncCache<Object, Object> getAsyncCache(String cacheName);

    LoadingCache<Object, Object> getLoadingCache(String cacheName, CacheLoader<Object, Object> loader);

    AsyncLoadingCache<Object, Object> getAsyncLoadingCache(String cacheName, CacheLoader<Object, Object> loader);

    AsyncLoadingCache<Object, Object> getAsyncLoadingCache(String cacheName, AsyncCacheLoader<Object, Object> loader);

    CaffeineCacheSpec resolveSpec(String cacheName);

    Set<String> cacheNames();

    void destroyCache(String cacheName);

    void destroyAllCaches();

    default CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor());
    }

    default <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor());
    }

    // -------------------- Typed wrappers --------------------

    default <K, V> Caffeine<K, V> typedBuilder(String cacheName) {
        return castBuilder(newBuilder(cacheName));
    }

    default <K, V> Cache<K, V> typedCache(String cacheName) {
        return castCache(getCache(cacheName));
    }

    default <K, V> AsyncCache<K, V> typedAsyncCache(String cacheName) {
        return castAsyncCache(getAsyncCache(cacheName));
    }

    default <K, V> LoadingCache<K, V> typedLoadingCache(String cacheName, CacheLoader<K, V> loader) {
        Objects.requireNonNull(loader, "loader");
        return castLoadingCache(getLoadingCache(cacheName, castCacheLoader(loader)));
    }

    default <K, V> AsyncLoadingCache<K, V> typedAsyncLoadingCache(String cacheName, CacheLoader<K, V> loader) {
        Objects.requireNonNull(loader, "loader");
        return castAsyncLoadingCache(getAsyncLoadingCache(cacheName, castCacheLoader(loader)));
    }

    default <K, V> AsyncLoadingCache<K, V> typedAsyncLoadingCache(String cacheName, AsyncCacheLoader<K, V> loader) {
        Objects.requireNonNull(loader, "loader");
        return castAsyncLoadingCache(getAsyncLoadingCache(cacheName, castAsyncCacheLoader(loader)));
    }

    default <K, V> Policy<K, V> typedPolicy(String cacheName) {
        return castPolicy(policy(cacheName));
    }

    default <K, V> V typedGetIfPresent(String cacheName, K key) {
        return this.<K, V>typedCache(cacheName).getIfPresent(key);
    }

    default <K, V> V typedGet(String cacheName, K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "mappingFunction");
        return this.<K, V>typedCache(cacheName).get(key, mappingFunction);
    }

    default <K, V> void typedPut(String cacheName, K key, V value) {
        typedCache(cacheName).put(key, value);
    }

    default <K, V> CompletableFuture<V> typedAsyncGet(String cacheName, K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "mappingFunction");
        return this.<K, V>typedAsyncCache(cacheName).get(key, mappingFunction);
    }

    default <K, V> V typedLoadingGet(String cacheName, K key, CacheLoader<K, V> loader) {
        return this.<K, V>typedLoadingCache(cacheName, loader).get(key);
    }

    default <K, V> CompletableFuture<V> typedAsyncLoadingGet(String cacheName, K key, CacheLoader<K, V> loader) {
        return this.<K, V>typedAsyncLoadingCache(cacheName, loader).get(key);
    }

    default <K, V> CompletableFuture<V> typedAsyncLoadingGet(String cacheName, K key, AsyncCacheLoader<K, V> loader) {
        return this.<K, V>typedAsyncLoadingCache(cacheName, loader).get(key);
    }

    // -------------------- Cache wrappers --------------------

    default Object getIfPresent(String cacheName, Object key) {
        return getCache(cacheName).getIfPresent(key);
    }

    default Object getIfPresentQuietly(String cacheName, Object key) {
        return policy(cacheName).getIfPresentQuietly(key);
    }

    default Policy.CacheEntry<Object, Object> getEntryIfPresentQuietly(String cacheName, Object key) {
        return policy(cacheName).getEntryIfPresentQuietly(key);
    }

    default Object get(String cacheName, Object key, Function<Object, Object> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "mappingFunction");
        return getCache(cacheName).get(key, mappingFunction::apply);
    }

    default Map<Object, Object> getAllPresent(String cacheName, Iterable<?> keys) {
        return getCache(cacheName).getAllPresent(castIterable(keys));
    }

    default Map<Object, Object> getAll(String cacheName,
                                       Iterable<?> keys,
                                       Function<? super Set<? extends Object>, ? extends Map<? extends Object, ? extends Object>> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "mappingFunction");
        return getCache(cacheName).getAll(castIterable(keys), mappingFunction);
    }

    default void put(String cacheName, Object key, Object value) {
        getCache(cacheName).put(key, value);
    }

    default void putAll(String cacheName, Map<?, ?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        getCache(cacheName).putAll(castMap(values));
    }

    default void invalidate(String cacheName, Object key) {
        getCache(cacheName).invalidate(key);
    }

    default void invalidateAll(String cacheName) {
        getCache(cacheName).invalidateAll();
    }

    default void invalidateAll(String cacheName, Iterable<?> keys) {
        getCache(cacheName).invalidateAll(castIterable(keys));
    }

    default long estimatedSize(String cacheName) {
        return getCache(cacheName).estimatedSize();
    }

    default ConcurrentMap<Object, Object> asMap(String cacheName) {
        return getCache(cacheName).asMap();
    }

    default void cleanUp(String cacheName) {
        getCache(cacheName).cleanUp();
    }

    default CacheStats stats(String cacheName) {
        return getCache(cacheName).stats();
    }

    default Policy<Object, Object> policy(String cacheName) {
        return getCache(cacheName).policy();
    }

    // -------------------- Policy wrappers --------------------

    default Optional<Policy.Eviction<Object, Object>> evictionPolicy(String cacheName) {
        return policy(cacheName).eviction();
    }

    default Optional<Policy.FixedExpiration<Object, Object>> expireAfterAccessPolicy(String cacheName) {
        return policy(cacheName).expireAfterAccess();
    }

    default Optional<Policy.FixedExpiration<Object, Object>> expireAfterWritePolicy(String cacheName) {
        return policy(cacheName).expireAfterWrite();
    }

    default Optional<Policy.VarExpiration<Object, Object>> variableExpirationPolicy(String cacheName) {
        return policy(cacheName).expireVariably();
    }

    default Optional<Policy.FixedRefresh<Object, Object>> refreshAfterWritePolicy(String cacheName) {
        return policy(cacheName).refreshAfterWrite();
    }

    default boolean isRecordingStats(String cacheName) {
        return policy(cacheName).isRecordingStats();
    }

    default Map<Object, CompletableFuture<Object>> refreshes(String cacheName) {
        return policy(cacheName).refreshes();
    }

    // -------------------- Async cache wrappers --------------------

    default CompletableFuture<Object> asyncGetIfPresent(String cacheName, Object key) {
        return getAsyncCache(cacheName).getIfPresent(key);
    }

    default CompletableFuture<Object> asyncGet(String cacheName,
                                               Object key,
                                               Function<? super Object, ? extends Object> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "mappingFunction");
        return getAsyncCache(cacheName).get(key, mappingFunction);
    }

    default CompletableFuture<Object> asyncGet(String cacheName,
                                               Object key,
                                               BiFunction<? super Object, ? super Executor, ? extends CompletableFuture<? extends Object>> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "mappingFunction");
        return getAsyncCache(cacheName).get(key, mappingFunction);
    }

    default CompletableFuture<Map<Object, Object>> asyncGetAll(
            String cacheName,
            Iterable<?> keys,
            Function<? super Set<? extends Object>, ? extends Map<? extends Object, ? extends Object>> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "mappingFunction");
        return getAsyncCache(cacheName).getAll(castIterable(keys), mappingFunction);
    }

    default CompletableFuture<Map<Object, Object>> asyncGetAll(
            String cacheName,
            Iterable<?> keys,
            BiFunction<? super Set<? extends Object>, ? super Executor,
                    ? extends CompletableFuture<? extends Map<? extends Object, ? extends Object>>> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "mappingFunction");
        return getAsyncCache(cacheName).getAll(castIterable(keys), mappingFunction);
    }

    default void asyncPut(String cacheName, Object key, CompletableFuture<? extends Object> valueFuture) {
        Objects.requireNonNull(valueFuture, "valueFuture");
        getAsyncCache(cacheName).put(key, valueFuture);
    }

    default ConcurrentMap<Object, CompletableFuture<Object>> asyncAsMap(String cacheName) {
        return getAsyncCache(cacheName).asMap();
    }

    default Cache<Object, Object> synchronous(String cacheName) {
        return getAsyncCache(cacheName).synchronous();
    }

    // -------------------- Loading cache wrappers --------------------

    default Object loadingGet(String cacheName, Object key, CacheLoader<Object, Object> loader) {
        return getLoadingCache(cacheName, loader).get(key);
    }

    default Map<Object, Object> loadingGetAll(String cacheName, Iterable<?> keys, CacheLoader<Object, Object> loader) {
        return getLoadingCache(cacheName, loader).getAll(castIterable(keys));
    }

    default CompletableFuture<Object> loadingRefresh(String cacheName, Object key, CacheLoader<Object, Object> loader) {
        return getLoadingCache(cacheName, loader).refresh(key);
    }

    default CompletableFuture<Map<Object, Object>> loadingRefreshAll(String cacheName,
                                                                     Iterable<?> keys,
                                                                     CacheLoader<Object, Object> loader) {
        return getLoadingCache(cacheName, loader).refreshAll(castIterable(keys));
    }

    // -------------------- Async loading cache wrappers --------------------

    default CompletableFuture<Object> asyncLoadingGet(String cacheName, Object key, CacheLoader<Object, Object> loader) {
        return getAsyncLoadingCache(cacheName, loader).get(key);
    }

    default CompletableFuture<Object> asyncLoadingGet(String cacheName, Object key, AsyncCacheLoader<Object, Object> loader) {
        return getAsyncLoadingCache(cacheName, loader).get(key);
    }

    default CompletableFuture<Map<Object, Object>> asyncLoadingGetAll(String cacheName,
                                                                      Iterable<?> keys,
                                                                      CacheLoader<Object, Object> loader) {
        return getAsyncLoadingCache(cacheName, loader).getAll(castIterable(keys));
    }

    default CompletableFuture<Map<Object, Object>> asyncLoadingGetAll(String cacheName,
                                                                      Iterable<?> keys,
                                                                      AsyncCacheLoader<Object, Object> loader) {
        return getAsyncLoadingCache(cacheName, loader).getAll(castIterable(keys));
    }

    default LoadingCache<Object, Object> asyncLoadingSynchronous(String cacheName, CacheLoader<Object, Object> loader) {
        return getAsyncLoadingCache(cacheName, loader).synchronous();
    }

    default LoadingCache<Object, Object> asyncLoadingSynchronous(String cacheName, AsyncCacheLoader<Object, Object> loader) {
        return getAsyncLoadingCache(cacheName, loader).synchronous();
    }

    // -------------------- Backward-compatible helpers --------------------

    default CompletableFuture<Object> getAsync(String cacheName, Object key, Function<Object, Object> mappingFunction) {
        return asyncGet(cacheName, key, mappingFunction);
    }

    default CompletableFuture<Object> getAsync(String cacheName, Object key, Supplier<Object> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return asyncGet(cacheName, key, ignored -> supplier.get());
    }

    @Override
    void close();

    @SuppressWarnings("unchecked")
    private static Iterable<Object> castIterable(Iterable<?> values) {
        return values == null ? Set.of() : (Iterable<Object>) values;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> castMap(Map<?, ?> values) {
        return (Map<Object, Object>) values;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Caffeine<K, V> castBuilder(Caffeine<?, ?> builder) {
        return (Caffeine<K, V>) builder;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Cache<K, V> castCache(Cache<?, ?> cache) {
        return (Cache<K, V>) cache;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> AsyncCache<K, V> castAsyncCache(AsyncCache<?, ?> cache) {
        return (AsyncCache<K, V>) cache;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> LoadingCache<K, V> castLoadingCache(LoadingCache<?, ?> cache) {
        return (LoadingCache<K, V>) cache;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> AsyncLoadingCache<K, V> castAsyncLoadingCache(AsyncLoadingCache<?, ?> cache) {
        return (AsyncLoadingCache<K, V>) cache;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Policy<K, V> castPolicy(Policy<?, ?> policy) {
        return (Policy<K, V>) policy;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> CacheLoader<Object, Object> castCacheLoader(CacheLoader<K, V> loader) {
        return (CacheLoader<Object, Object>) loader;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> AsyncCacheLoader<Object, Object> castAsyncCacheLoader(AsyncCacheLoader<K, V> loader) {
        return (AsyncCacheLoader<Object, Object>) loader;
    }
}
