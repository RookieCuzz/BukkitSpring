package com.cuzz.starter.bukkitspring.caffeine.config;

import com.cuzz.starter.bukkitspring.caffeine.api.CaffeineCacheSpec;
import com.cuzz.starter.bukkitspring.caffeine.testutil.MapConfigView;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaffeineSettingsTest {

    @Test
    public void defaultsWhenConfigMissing() {
        CaffeineSettings settings = CaffeineSettings.fromConfig(null);

        assertFalse(settings.enabled);
        assertTrue(settings.useVirtualThreads);
        assertEquals("default", settings.defaultCacheName);
        assertEquals(64, settings.defaultCacheSpec.initialCapacity);
        assertEquals(10_000L, settings.defaultCacheSpec.maximumSize);
        assertEquals(0L, settings.defaultCacheSpec.expireAfterWriteMillis);
        assertEquals(0L, settings.defaultCacheSpec.expireAfterAccessMillis);
        assertEquals(0L, settings.defaultCacheSpec.refreshAfterWriteMillis);
        assertTrue(settings.namedCacheSpecs.isEmpty());
    }

    @Test
    public void parsesNamedCachesAndClampsValues() {
        CaffeineSettings settings = CaffeineSettings.fromConfig(new MapConfigView(Map.of(
                "caffeine", Map.of(
                        "enabled", true,
                        "virtual-threads", false,
                        "default-cache-name", "primary",
                        "default", Map.of(
                                "initial-capacity", -1,
                                "maximum-size", 999_999_999_999L,
                                "expire-after-write-ms", 5000L,
                                "expire-after-access-ms", -2L,
                                "refresh-after-write-ms", 1600L,
                                "record-stats", true
                        ),
                        "caches", Map.of(
                                "hot", Map.of(
                                        "maximum-size", 50L,
                                        "expire-after-access-ms", 1500L,
                                        "refresh-after-write-ms", 1800L,
                                        "weak-values", true,
                                        "soft-values", true
                                ),
                                "cold", Map.of(
                                        "maximum-size", 200L,
                                        "expire-after-write-ms", 10_000L
                                )
                        )
                )
        )));

        assertTrue(settings.enabled);
        assertFalse(settings.useVirtualThreads);
        assertEquals("primary", settings.defaultCacheName);

        CaffeineCacheSpec defaultSpec = settings.defaultCacheSpec;
        assertEquals(0, defaultSpec.initialCapacity);
        assertEquals(100_000_000L, defaultSpec.maximumSize);
        assertEquals(5000L, defaultSpec.expireAfterWriteMillis);
        assertEquals(0L, defaultSpec.expireAfterAccessMillis);
        assertEquals(1600L, defaultSpec.refreshAfterWriteMillis);
        assertTrue(defaultSpec.recordStats);

        assertEquals(2, settings.namedCacheSpecs.size());

        CaffeineCacheSpec hotSpec = settings.specFor("hot");
        assertEquals(50L, hotSpec.maximumSize);
        assertEquals(1500L, hotSpec.expireAfterAccessMillis);
        assertEquals(1800L, hotSpec.refreshAfterWriteMillis);
        assertTrue(hotSpec.weakValues);
        assertFalse(hotSpec.softValues);

        CaffeineCacheSpec unknownSpec = settings.specFor("unknown");
        assertEquals(defaultSpec.maximumSize, unknownSpec.maximumSize);
        assertEquals(defaultSpec.expireAfterWriteMillis, unknownSpec.expireAfterWriteMillis);
    }

    @Test
    public void blankDefaultCacheNameFallsBack() {
        CaffeineSettings settings = CaffeineSettings.fromConfig(new MapConfigView(Map.of(
                "caffeine", Map.of(
                        "default-cache-name", "   "
                )
        )));

        assertEquals("default", settings.defaultCacheName);
    }
}
