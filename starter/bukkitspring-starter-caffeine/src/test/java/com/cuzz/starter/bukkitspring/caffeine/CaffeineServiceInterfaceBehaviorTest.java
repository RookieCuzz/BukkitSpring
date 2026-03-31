package com.cuzz.starter.bukkitspring.caffeine;

import com.cuzz.starter.bukkitspring.caffeine.config.CaffeineSettings;
import com.cuzz.starter.bukkitspring.caffeine.internal.DefaultCaffeineService;
import com.cuzz.starter.bukkitspring.caffeine.testutil.MapConfigView;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.CacheLoader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaffeineServiceInterfaceBehaviorTest {

    @Test
    public void cacheAndPolicyWrappersShouldWork() {
        DefaultCaffeineService service = newService(Map.of(
                "enabled", true,
                "virtual-threads", false,
                "default", Map.of(
                        "maximum-size", 128L,
                        "expire-after-write-ms", 5_000L,
                        "expire-after-access-ms", 4_000L,
                        "record-stats", true
                )
        ));

        service.putAll("ops", Map.of("a", 1, "b", 2));
        assertEquals(2, service.getAllPresent("ops", List.of("a", "b")).size());
        assertEquals(3, service.get("ops", "c", ignored -> 3));

        Map<Object, Object> loaded = service.getAll("ops", List.of("d", "e"), keys -> {
            Map<Object, Object> values = new HashMap<>();
            for (Object key : keys) {
                values.put(key, key + "-value");
            }
            return values;
        });
        assertEquals("d-value", loaded.get("d"));
        assertEquals("e-value", loaded.get("e"));

        service.invalidateAll("ops", List.of("a"));
        assertNull(service.getIfPresent("ops", "a"));
        assertNotNull(service.getEntryIfPresentQuietly("ops", "b"));
        assertTrue(service.asMap("ops").containsKey("b"));
        assertTrue(service.estimatedSize("ops") >= 1L);

        service.cleanUp("ops");
        assertNotNull(service.stats("ops"));
        assertNotNull(service.policy("ops"));
        assertTrue(service.evictionPolicy("ops").isPresent());
        assertTrue(service.expireAfterAccessPolicy("ops").isPresent());
        assertTrue(service.expireAfterWritePolicy("ops").isPresent());
        assertFalse(service.variableExpirationPolicy("ops").isPresent());
        assertFalse(service.refreshAfterWritePolicy("ops").isPresent());
        assertTrue(service.isRecordingStats("ops"));

        service.invalidateAll("ops");
        assertEquals(0L, service.estimatedSize("ops"));
        service.close();
    }

    @Test
    public void asyncAndLoadingWrappersShouldWork() throws Exception {
        DefaultCaffeineService service = newService(Map.of(
                "enabled", true,
                "virtual-threads", false
        ));

        service.asyncPut("async", "k0", CompletableFuture.completedFuture("v0"));
        assertEquals("v0", service.asyncGetIfPresent("async", "k0").get(2, TimeUnit.SECONDS));
        assertEquals("v1", service.asyncGet("async", "k1", ignored -> "v1").get(2, TimeUnit.SECONDS));
        assertEquals("v2", service.asyncGet("async", "k2",
                (ignored, executor) -> CompletableFuture.supplyAsync(() -> "v2", executor))
                .get(2, TimeUnit.SECONDS));

        Map<Object, Object> asyncBatch1 = service.asyncGetAll("async", List.of("k3", "k4"), keys -> {
            Map<Object, Object> values = new HashMap<>();
            for (Object key : keys) {
                values.put(key, key + "-mapped");
            }
            return values;
        }).get(2, TimeUnit.SECONDS);
        assertEquals("k3-mapped", asyncBatch1.get("k3"));

        Map<Object, Object> asyncBatch2 = service.asyncGetAll("async", List.of("k5"),
                (keys, executor) -> CompletableFuture.supplyAsync(() -> Map.of("k5", "v5"), executor))
                .get(2, TimeUnit.SECONDS);
        assertEquals("v5", asyncBatch2.get("k5"));

        assertTrue(service.asyncAsMap("async").containsKey("k0"));
        assertEquals("v0", service.synchronous("async").getIfPresent("k0"));

        CacheLoader<Object, Object> loader = key -> "L-" + key;
        assertEquals("L-x", service.loadingGet("loading", "x", loader));
        Map<Object, Object> loadAll = service.loadingGetAll("loading", List.of("x", "y"), loader);
        assertEquals("L-y", loadAll.get("y"));
        assertEquals("L-x", service.loadingRefresh("loading", "x", loader).get(2, TimeUnit.SECONDS));
        Map<Object, Object> refreshed = service.loadingRefreshAll("loading", List.of("x", "y"), loader)
                .get(2, TimeUnit.SECONDS);
        assertTrue(refreshed.containsKey("x"));

        assertEquals("L-z", service.asyncLoadingGet("aloading-sync-loader", "z", loader)
                .get(2, TimeUnit.SECONDS));
        Map<Object, Object> asyncLoadAllBySyncLoader = service.asyncLoadingGetAll(
                "aloading-sync-loader",
                List.of("m", "n"),
                loader
        ).get(2, TimeUnit.SECONDS);
        assertEquals("L-m", asyncLoadAllBySyncLoader.get("m"));

        AsyncCacheLoader<Object, Object> asyncLoader =
                (key, executor) -> CompletableFuture.supplyAsync(() -> "A-" + key, executor);
        assertEquals("A-q", service.asyncLoadingGet("aloading-async-loader", "q", asyncLoader)
                .get(2, TimeUnit.SECONDS));
        Map<Object, Object> asyncLoadAllByAsyncLoader = service.asyncLoadingGetAll(
                "aloading-async-loader",
                List.of("p"),
                asyncLoader
        ).get(2, TimeUnit.SECONDS);
        assertEquals("A-p", asyncLoadAllByAsyncLoader.get("p"));

        assertNotNull(service.asyncLoadingSynchronous("aloading-sync-loader", loader));
        assertNotNull(service.asyncLoadingSynchronous("aloading-async-loader", asyncLoader));
        service.close();
    }

    @Test
    public void managementAndExecutorHelpersShouldWork() throws Exception {
        DefaultCaffeineService service = newService(Map.of(
                "enabled", true,
                "virtual-threads", false,
                "default-cache-name", "primary"
        ));

        assertEquals("primary", service.settings().defaultCacheName);
        service.put("cache-a", "x", "1");
        service.put("cache-b", "y", "2");
        assertTrue(service.cacheNames().contains("primary"));
        assertTrue(service.cacheNames().containsAll(Set.of("cache-a", "cache-b")));

        AtomicInteger counter = new AtomicInteger();
        service.runAsync(counter::incrementAndGet).get(2, TimeUnit.SECONDS);
        assertEquals(1, counter.get());
        assertEquals(42, service.supplyAsync(() -> 42).get(2, TimeUnit.SECONDS));

        service.destroyAllCaches();
        assertNull(service.getIfPresent("cache-a", "x"));
        assertNull(service.getIfPresent("cache-b", "y"));
        service.close();
    }

    private static DefaultCaffeineService newService(Map<String, Object> caffeineSection) {
        CaffeineSettings settings = CaffeineSettings.fromConfig(new MapConfigView(Map.of(
                "caffeine", caffeineSection
        )));
        return new DefaultCaffeineService(settings, Logger.getLogger("test"));
    }
}
