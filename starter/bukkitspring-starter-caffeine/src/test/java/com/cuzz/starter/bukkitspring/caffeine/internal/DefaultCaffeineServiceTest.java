package com.cuzz.starter.bukkitspring.caffeine.internal;

import com.cuzz.starter.bukkitspring.caffeine.config.CaffeineSettings;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.cuzz.starter.bukkitspring.caffeine.testutil.MapConfigView;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultCaffeineServiceTest {

    @Test
    public void putGetAndDestroyCache() {
        CaffeineSettings settings = CaffeineSettings.fromConfig(new MapConfigView(Map.of(
                "caffeine", Map.of(
                        "enabled", true,
                        "virtual-threads", false
                )
        )));
        DefaultCaffeineService service = new DefaultCaffeineService(settings, Logger.getLogger("test"));

        service.put("players", "u1", "Alice");
        assertEquals("Alice", service.getIfPresent("players", "u1"));
        assertTrue(service.cacheNames().contains("players"));

        service.destroyCache("players");
        assertNull(service.getIfPresent("players", "u1"));
    }

    @Test
    public void resolveNamedSpecAndLoadAsync() throws Exception {
        CaffeineSettings settings = CaffeineSettings.fromConfig(new MapConfigView(Map.of(
                "caffeine", Map.of(
                        "enabled", true,
                        "virtual-threads", false,
                        "caches", Map.of(
                                "hot", Map.of(
                                        "maximum-size", 50L,
                                        "expire-after-write-ms", 1000L
                                )
                        )
                )
        )));
        DefaultCaffeineService service = new DefaultCaffeineService(settings, Logger.getLogger("test"));

        assertEquals(50L, service.resolveSpec("hot").maximumSize);
        Object value = service.getAsync("hot", "k1", key -> "v1").get(3, TimeUnit.SECONDS);
        assertEquals("v1", value);
        assertEquals("v1", service.getIfPresent("hot", "k1"));
        assertTrue(service.expireAfterWritePolicy("hot").isPresent());
    }

    @Test
    public void loadingCachesWrapNativeInterfaces() throws Exception {
        CaffeineSettings settings = CaffeineSettings.fromConfig(new MapConfigView(Map.of(
                "caffeine", Map.of(
                        "enabled", true,
                        "virtual-threads", false
                )
        )));
        DefaultCaffeineService service = new DefaultCaffeineService(settings, Logger.getLogger("test"));

        AtomicInteger syncLoads = new AtomicInteger();
        CacheLoader<Object, Object> loader = key -> {
            syncLoads.incrementAndGet();
            return "sync-" + key;
        };

        Object value1 = service.loadingGet("loader-sync", "k1", loader);
        Object value2 = service.loadingGet("loader-sync", "k1", loader);
        assertEquals("sync-k1", value1);
        assertEquals("sync-k1", value2);
        assertEquals(1, syncLoads.get());

        AtomicInteger asyncLoads = new AtomicInteger();
        AsyncCacheLoader<Object, Object> asyncLoader = (key, executor) -> {
            asyncLoads.incrementAndGet();
            return java.util.concurrent.CompletableFuture.supplyAsync(() -> "async-" + key, executor);
        };

        Object asyncValue = service.asyncLoadingGet("loader-async", "k2", asyncLoader).get(3, TimeUnit.SECONDS);
        assertEquals("async-k2", asyncValue);
        assertTrue(asyncLoads.get() >= 1);

        assertTrue(service.cacheNames().contains("loader-sync"));
        assertTrue(service.cacheNames().contains("loader-async"));
    }

    @Test
    public void exposesPolicyAndStats() {
        CaffeineSettings settings = CaffeineSettings.fromConfig(new MapConfigView(Map.of(
                "caffeine", Map.of(
                        "enabled", true,
                        "virtual-threads", false,
                        "default", Map.of(
                                "maximum-size", 100L,
                                "record-stats", true
                        )
                )
        )));
        DefaultCaffeineService service = new DefaultCaffeineService(settings, Logger.getLogger("test"));

        service.put("policy", "a", "A");
        assertEquals("A", service.getIfPresentQuietly("policy", "a"));

        Optional<com.github.benmanes.caffeine.cache.Policy.Eviction<Object, Object>> eviction =
                service.evictionPolicy("policy");
        assertTrue(eviction.isPresent());
        assertTrue(service.isRecordingStats("policy"));
        assertNotNull(service.stats("policy"));
    }

    @Test
    public void typedApisProvideTypeSafeView() throws Exception {
        CaffeineSettings settings = CaffeineSettings.fromConfig(new MapConfigView(Map.of(
                "caffeine", Map.of(
                        "enabled", true,
                        "virtual-threads", false
                )
        )));
        DefaultCaffeineService service = new DefaultCaffeineService(settings, Logger.getLogger("test"));

        service.typedPut("typed", "k1", 123);
        Integer value = service.typedGetIfPresent("typed", "k1");
        assertEquals(123, value);

        Integer loaded = service.typedGet("typed", "k2", key -> key.length()).intValue();
        assertEquals(2, loaded);

        Integer asyncLoaded = service.typedAsyncGet("typed", "k3", key -> key.length()).get(3, TimeUnit.SECONDS);
        assertEquals(2, asyncLoaded);

        CacheLoader<String, Integer> loader = String::length;
        Integer loadingValue = service.typedLoadingGet("typed-loading", "abcd", loader);
        assertEquals(4, loadingValue);

        Integer asyncLoadingValue = service.typedAsyncLoadingGet("typed-async-loading", "abcde", loader)
                .get(3, TimeUnit.SECONDS);
        assertEquals(5, asyncLoadingValue);
    }

    @Test
    public void shouldRejectDifferentLoaderTypeForSameLoadingCacheName() {
        CaffeineSettings settings = CaffeineSettings.fromConfig(new MapConfigView(Map.of(
                "caffeine", Map.of(
                        "enabled", true,
                        "virtual-threads", false
                )
        )));
        DefaultCaffeineService service = new DefaultCaffeineService(settings, Logger.getLogger("test"));

        CacheLoader<Object, Object> loader1 = new CacheLoader<>() {
            @Override
            public Object load(Object key) {
                return "a-" + key;
            }
        };
        CacheLoader<Object, Object> loader2 = new CacheLoader<>() {
            @Override
            public Object load(Object key) {
                return "b-" + key;
            }
        };

        service.getLoadingCache("same-loader", loader1);
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.getLoadingCache("same-loader", loader2)
        );
        assertTrue(ex.getMessage().contains("same-loader"));
    }

    @Test
    public void disabledServiceRejectsCacheAccess() {
        CaffeineSettings settings = CaffeineSettings.fromConfig(new MapConfigView(Map.of(
                "caffeine", Map.of(
                        "enabled", false
                )
        )));
        DefaultCaffeineService service = new DefaultCaffeineService(settings, Logger.getLogger("test"));

        assertFalse(service.isEnabled());
        assertThrows(IllegalStateException.class, () -> service.getCache("any"));
    }

    @Test
    public void closeShutsDownExecutorAndBlocksFurtherUsage() {
        CaffeineSettings settings = CaffeineSettings.fromConfig(new MapConfigView(Map.of(
                "caffeine", Map.of(
                        "enabled", true,
                        "virtual-threads", false
                )
        )));
        DefaultCaffeineService service = new DefaultCaffeineService(settings, Logger.getLogger("test"));

        ExecutorService executor = service.executor();
        assertNotNull(executor);

        service.close();
        assertTrue(executor.isShutdown());
        assertFalse(service.isEnabled());
        assertThrows(IllegalStateException.class, () -> service.getCache("any"));
    }
}
