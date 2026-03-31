package com.cuzz.starter.bukkitspring.caffeine.internal;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.bukkitspring.api.annotation.PreDestroy;
import com.cuzz.starter.bukkitspring.caffeine.api.CaffeineService;
import com.cuzz.starter.bukkitspring.caffeine.config.CaffeineSettings;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Component
public final class DefaultCaffeineService implements CaffeineService {
    private final CaffeineSettings settings;
    private final Logger logger;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object executorLock = new Object();
    private final ConcurrentMap<String, AsyncCache<Object, Object>> asyncCaches = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Cache<Object, Object>> syncViews = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LoadingCache<Object, Object>> loadingCaches = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AsyncLoadingCache<Object, Object>> asyncLoadingCaches = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> loadingLoaderTypes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> asyncLoadingLoaderTypes = new ConcurrentHashMap<>();
    private volatile ExecutorService executor;

    @Autowired
    public DefaultCaffeineService(CaffeineSettings settings, Logger logger) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = logger;
    }

    @PostConstruct
    public void registerGlobalBean() {
        if (!settings.enabled) {
            logInfo("[Caffeine] Disabled, skip global bean registration.");
            return;
        }
        registerGlobalBeanInternal();
        precreateConfiguredCaches();
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
    public CaffeineSettings settings() {
        return settings;
    }

    @Override
    public ExecutorService executor() {
        if (closed.get()) {
            throw new IllegalStateException("Caffeine service is closed.");
        }
        ExecutorService current = executor;
        if (current != null) {
            return current;
        }
        synchronized (executorLock) {
            if (executor == null) {
                executor = CaffeineExecutors.create(settings.useVirtualThreads, logger);
            }
            return executor;
        }
    }

    @Override
    public Caffeine<Object, Object> newBuilder(String cacheName) {
        ensureEnabled();
        return settings.specFor(normalizeCacheName(cacheName))
                .newBuilder()
                .executor(executor());
    }

    @Override
    public Cache<Object, Object> defaultCache() {
        return getCache(settings.defaultCacheName);
    }

    @Override
    public Cache<Object, Object> getCache(String cacheName) {
        ensureEnabled();
        String normalized = normalizeCacheName(cacheName);
        return syncViews.computeIfAbsent(normalized, name -> getAsyncCache(name).synchronous());
    }

    @Override
    public AsyncCache<Object, Object> getAsyncCache(String cacheName) {
        ensureEnabled();
        String normalized = normalizeCacheName(cacheName);
        return asyncCaches.computeIfAbsent(normalized, this::createAsyncCache);
    }

    @Override
    public LoadingCache<Object, Object> getLoadingCache(String cacheName, CacheLoader<Object, Object> loader) {
        ensureEnabled();
        Objects.requireNonNull(loader, "loader");
        String normalized = normalizeCacheName(cacheName);
        ensureLoaderTypeCompatible(loadingLoaderTypes, normalized, loader, "LoadingCache");
        return loadingCaches.computeIfAbsent(normalized, name -> createLoadingCache(name, loader));
    }

    @Override
    public AsyncLoadingCache<Object, Object> getAsyncLoadingCache(String cacheName, CacheLoader<Object, Object> loader) {
        ensureEnabled();
        Objects.requireNonNull(loader, "loader");
        String normalized = normalizeCacheName(cacheName);
        ensureLoaderTypeCompatible(asyncLoadingLoaderTypes, normalized, loader, "AsyncLoadingCache");
        return asyncLoadingCaches.computeIfAbsent(normalized, name -> createAsyncLoadingCache(name, loader));
    }

    @Override
    public AsyncLoadingCache<Object, Object> getAsyncLoadingCache(String cacheName, AsyncCacheLoader<Object, Object> loader) {
        ensureEnabled();
        Objects.requireNonNull(loader, "loader");
        String normalized = normalizeCacheName(cacheName);
        ensureLoaderTypeCompatible(asyncLoadingLoaderTypes, normalized, loader, "AsyncLoadingCache");
        return asyncLoadingCaches.computeIfAbsent(normalized, name -> createAsyncLoadingCache(name, loader));
    }

    @Override
    public com.cuzz.starter.bukkitspring.caffeine.api.CaffeineCacheSpec resolveSpec(String cacheName) {
        ensureEnabled();
        return settings.specFor(normalizeCacheName(cacheName));
    }

    @Override
    public Set<String> cacheNames() {
        ensureEnabled();
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(settings.defaultCacheName);
        names.addAll(asyncCaches.keySet());
        names.addAll(syncViews.keySet());
        names.addAll(loadingCaches.keySet());
        names.addAll(asyncLoadingCaches.keySet());
        return Collections.unmodifiableSet(names);
    }

    @Override
    public void destroyCache(String cacheName) {
        ensureEnabled();
        String normalized = normalizeCacheName(cacheName);

        Cache<Object, Object> removedView = syncViews.remove(normalized);
        if (removedView != null) {
            removedView.invalidateAll();
        }

        AsyncCache<Object, Object> removedAsync = asyncCaches.remove(normalized);
        if (removedAsync != null) {
            removedAsync.synchronous().invalidateAll();
        }

        LoadingCache<Object, Object> removedLoading = loadingCaches.remove(normalized);
        if (removedLoading != null) {
            removedLoading.invalidateAll();
        }
        loadingLoaderTypes.remove(normalized);

        AsyncLoadingCache<Object, Object> removedAsyncLoading = asyncLoadingCaches.remove(normalized);
        if (removedAsyncLoading != null) {
            removedAsyncLoading.synchronous().invalidateAll();
        }
        asyncLoadingLoaderTypes.remove(normalized);
    }

    @Override
    public void destroyAllCaches() {
        ensureEnabled();

        for (Cache<Object, Object> cache : syncViews.values()) {
            cache.invalidateAll();
        }
        syncViews.clear();

        for (AsyncCache<Object, Object> cache : asyncCaches.values()) {
            cache.synchronous().invalidateAll();
        }
        asyncCaches.clear();

        for (LoadingCache<Object, Object> cache : loadingCaches.values()) {
            cache.invalidateAll();
        }
        loadingCaches.clear();
        loadingLoaderTypes.clear();

        for (AsyncLoadingCache<Object, Object> cache : asyncLoadingCaches.values()) {
            cache.synchronous().invalidateAll();
        }
        asyncLoadingCaches.clear();
        asyncLoadingLoaderTypes.clear();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        clearCachesQuietly();

        ExecutorService currentExecutor = executor;
        if (currentExecutor != null) {
            currentExecutor.shutdown();
        }
    }

    private AsyncCache<Object, Object> createAsyncCache(String cacheName) {
        return settings.specFor(cacheName)
                .newBuilder()
                .executor(executor())
                .buildAsync();
    }

    private LoadingCache<Object, Object> createLoadingCache(String cacheName, CacheLoader<Object, Object> loader) {
        return settings.specFor(cacheName)
                .newBuilder()
                .executor(executor())
                .build(loader);
    }

    private AsyncLoadingCache<Object, Object> createAsyncLoadingCache(String cacheName, CacheLoader<Object, Object> loader) {
        return settings.specFor(cacheName)
                .newBuilder()
                .executor(executor())
                .buildAsync(loader);
    }

    private AsyncLoadingCache<Object, Object> createAsyncLoadingCache(String cacheName, AsyncCacheLoader<Object, Object> loader) {
        return settings.specFor(cacheName)
                .newBuilder()
                .executor(executor())
                .buildAsync(loader);
    }

    private void precreateConfiguredCaches() {
        getCache(settings.defaultCacheName);
        for (String cacheName : settings.namedCacheSpecs.keySet()) {
            getCache(cacheName);
        }
        if (!settings.namedCacheSpecs.isEmpty()) {
            logInfo("[Caffeine] Precreated named caches: " + settings.namedCacheSpecs.keySet());
        }
    }

    private void clearCachesQuietly() {
        for (Cache<Object, Object> cache : syncViews.values()) {
            cache.invalidateAll();
        }
        syncViews.clear();

        for (AsyncCache<Object, Object> cache : asyncCaches.values()) {
            cache.synchronous().invalidateAll();
        }
        asyncCaches.clear();

        for (LoadingCache<Object, Object> cache : loadingCaches.values()) {
            cache.invalidateAll();
        }
        loadingCaches.clear();
        loadingLoaderTypes.clear();

        for (AsyncLoadingCache<Object, Object> cache : asyncLoadingCaches.values()) {
            cache.synchronous().invalidateAll();
        }
        asyncLoadingCaches.clear();
        asyncLoadingLoaderTypes.clear();
    }

    private void ensureLoaderTypeCompatible(ConcurrentMap<String, String> loaderTypes,
                                            String cacheName,
                                            Object loader,
                                            String cacheKind) {
        String incoming = loader.getClass().getName();
        String existing = loaderTypes.putIfAbsent(cacheName, incoming);
        if (existing != null && !existing.equals(incoming)) {
            throw new IllegalStateException(
                    cacheKind + " '" + cacheName + "' was already initialized with loader type '"
                            + existing + "' but received '" + incoming + "'."
            );
        }
    }

    private void ensureEnabled() {
        if (!settings.enabled) {
            throw new IllegalStateException("Caffeine is disabled (caffeine.enabled=false).");
        }
        if (closed.get()) {
            throw new IllegalStateException("Caffeine service is closed.");
        }
    }

    private String normalizeCacheName(String cacheName) {
        String normalized = normalize(cacheName);
        if (normalized.isEmpty()) {
            return settings.defaultCacheName;
        }
        return normalized;
    }

    private void registerGlobalBeanInternal() {
        try {
            Class<?> bukkitSpringClass = Class.forName("com.cuzz.bukkitspring.BukkitSpring");
            java.lang.reflect.Method registerMethod = bukkitSpringClass
                    .getMethod("registerGlobalBean", Class.class, Object.class);
            registerMethod.invoke(null, CaffeineService.class, this);
            logInfo("[Caffeine] CaffeineService registered as global bean");
        } catch (Exception e) {
            logWarning("[Caffeine] Failed to register global bean: " + e.getMessage());
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

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
