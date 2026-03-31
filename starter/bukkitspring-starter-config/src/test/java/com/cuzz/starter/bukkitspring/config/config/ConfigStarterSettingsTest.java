package com.cuzz.starter.bukkitspring.config.config;

import com.cuzz.starter.bukkitspring.config.api.ConfigLoadStrategy;
import com.cuzz.starter.bukkitspring.config.testutil.MapConfigView;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigStarterSettingsTest {

    @Test
    void shouldReadDefaultsWhenConfigMissing() {
        ConfigStarterSettings settings = ConfigStarterSettings.fromConfig(new MapConfigView(Map.of()));

        assertEquals(false, settings.enabled);
        assertEquals(ConfigLoadStrategy.LOCAL_FIRST, settings.defaultStrategy);
        assertEquals(true, settings.cacheEnabled);
        assertEquals(StandardCharsets.UTF_8, settings.localCharset);
        assertEquals(false, settings.remoteEnabled);
    }

    @Test
    void shouldParseRemoteHeadersAndStrategy() {
        Map<String, Object> values = Map.of(
                "config-starter.enabled", true,
                "config-starter.source.strategy", "remote-first",
                "config-starter.source.remote.enabled", true,
                "config-starter.source.remote.base-url", "http://127.0.0.1:12345",
                "config-starter.source.remote.headers.Authorization", "Bearer test",
                "config-starter.source.remote.headers.X-Tenant", "demo"
        );
        ConfigStarterSettings settings = ConfigStarterSettings.fromConfig(new MapConfigView(values));

        assertTrue(settings.enabled);
        assertEquals(ConfigLoadStrategy.REMOTE_FIRST, settings.defaultStrategy);
        assertTrue(settings.remoteEnabled);
        assertEquals("Bearer test", settings.remoteHeaders.get("Authorization"));
        assertEquals("demo", settings.remoteHeaders.get("X-Tenant"));
    }
}
