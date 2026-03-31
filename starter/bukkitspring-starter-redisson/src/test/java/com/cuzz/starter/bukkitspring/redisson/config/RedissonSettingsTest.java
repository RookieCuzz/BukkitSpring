package com.cuzz.starter.bukkitspring.redisson.config;

import com.cuzz.starter.bukkitspring.redisson.api.RedissonMode;
import com.cuzz.starter.bukkitspring.redisson.testutil.MapConfigView;
import org.junit.jupiter.api.Test;
import org.redisson.config.TransportMode;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RedissonSettingsTest {

    @Test
    public void defaultsWhenConfigMissing() {
        RedissonSettings settings = RedissonSettings.fromConfig(null);

        assertFalse(settings.enabled);
        assertTrue(settings.useVirtualThreads);
        assertEquals(RedissonMode.SINGLE, settings.mode);
        assertEquals("redis://127.0.0.1:6379", settings.address);
        assertEquals(0, settings.database);
        assertEquals(3000, settings.timeoutMillis);
        assertEquals(10_000, settings.connectTimeoutMillis);
        assertEquals(10_000, settings.idleConnectionTimeoutMillis);
        assertEquals(3, settings.retryAttempts);
        assertEquals(1500, settings.retryIntervalMillis);
        assertEquals(64, settings.connectionPoolSize);
        assertEquals(24, settings.connectionMinimumIdleSize);
        assertEquals(50, settings.subscriptionConnectionPoolSize);
        assertEquals(1, settings.subscriptionConnectionMinimumIdleSize);
        assertEquals(TransportMode.NIO, settings.transportMode);
        assertFalse(settings.cluster.enabled);
        assertTrue(settings.cluster.nodes.isEmpty());
        assertEquals(1000, settings.cluster.scanIntervalMillis);
    }

    @Test
    public void parsesAndClampsSingleValues() {
        RedissonSettings settings = RedissonSettings.fromConfig(new MapConfigView(Map.of(
                "redisson",
                Map.ofEntries(
                        Map.entry("enabled", true),
                        Map.entry("virtual-threads", false),
                        Map.entry("mode", "single"),
                        Map.entry("address", "127.0.0.1:6379"),
                        Map.entry("database", -100),
                        Map.entry("timeout-ms", 999_999),
                        Map.entry("connect-timeout-ms", -10),
                        Map.entry("idle-connection-timeout-ms", 0),
                        Map.entry("retry-attempts", 999),
                        Map.entry("retry-interval-ms", -10),
                        Map.entry("connection-pool-size", 9999),
                        Map.entry("connection-minimum-idle-size", -5),
                        Map.entry("subscription-connection-pool-size", -1),
                        Map.entry("subscription-connection-minimum-idle-size", 9999),
                        Map.entry("threads", 2048),
                        Map.entry("netty-threads", -5),
                        Map.entry("transport-mode", "kqueue")
                )
        )));

        assertTrue(settings.enabled);
        assertFalse(settings.useVirtualThreads);
        assertEquals(RedissonMode.SINGLE, settings.mode);
        assertEquals("redis://127.0.0.1:6379", settings.address);
        assertEquals(0, settings.database);
        assertEquals(120_000, settings.timeoutMillis);
        assertEquals(1, settings.connectTimeoutMillis);
        assertEquals(1, settings.idleConnectionTimeoutMillis);
        assertEquals(100, settings.retryAttempts);
        assertEquals(1, settings.retryIntervalMillis);
        assertEquals(1024, settings.connectionPoolSize);
        assertEquals(0, settings.connectionMinimumIdleSize);
        assertEquals(1, settings.subscriptionConnectionPoolSize);
        assertEquals(1024, settings.subscriptionConnectionMinimumIdleSize);
        assertEquals(1024, settings.threads);
        assertEquals(0, settings.nettyThreads);
        assertEquals(TransportMode.KQUEUE, settings.transportMode);
    }

    @Test
    public void clusterNodesShouldForceClusterModeAndNormalizeAddress() {
        RedissonSettings settings = RedissonSettings.fromConfig(new MapConfigView(Map.of(
                "redisson", Map.of(
                        "mode", "single",
                        "cluster", Map.of(
                                "enabled", false,
                                "nodes", List.of("10.0.0.1:6379", "redis://10.0.0.2:6379"),
                                "scan-interval-ms", 2500
                        )
                )
        )));

        assertEquals(RedissonMode.CLUSTER, settings.mode);
        assertEquals(List.of("redis://10.0.0.1:6379", "redis://10.0.0.2:6379"), settings.cluster.nodes);
        assertEquals(2500, settings.cluster.scanIntervalMillis);
    }

    @Test
    public void invalidTransportModeFallsBackToNio() {
        RedissonSettings settings = RedissonSettings.fromConfig(new MapConfigView(Map.of(
                "redisson", Map.of(
                        "transport-mode", "invalid-mode"
                )
        )));
        assertEquals(TransportMode.NIO, settings.transportMode);
    }
}
