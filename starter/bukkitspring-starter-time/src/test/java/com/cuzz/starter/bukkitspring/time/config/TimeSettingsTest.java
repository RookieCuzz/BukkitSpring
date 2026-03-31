package com.cuzz.starter.bukkitspring.time.config;

import com.cuzz.starter.bukkitspring.time.testutil.MapConfigView;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeSettingsTest {

    @Test
    public void defaultsWhenConfigMissing() {
        TimeSettings settings = TimeSettings.fromConfig(null);

        assertFalse(settings.enabled);
        assertTrue(settings.useVirtualThreads);
        assertEquals(ZoneId.systemDefault(), settings.zoneId);
        assertEquals(0L, settings.debugOffsetMillis);
        assertFalse(settings.allowSetSystemTime);
        assertEquals(5000L, settings.commandTimeoutMillis);
        assertTrue(settings.preferTimedatectl);
    }

    @Test
    public void parsesAndClampsValues() {
        TimeSettings settings = TimeSettings.fromConfig(new MapConfigView(Map.of(
                "time.enabled", true,
                "time.virtual-threads", false,
                "time.zone-id", "UTC",
                "time.debug-offset-minutes", 999999999999L,
                "time.system-time.allow-set", true,
                "time.system-time.command-timeout-ms", 10,
                "time.system-time.prefer-timedatectl", false
        )));

        assertTrue(settings.enabled);
        assertFalse(settings.useVirtualThreads);
        assertEquals(ZoneId.of("UTC"), settings.zoneId);
        assertEquals(315_576_000_000L, settings.debugOffsetMillis);
        assertTrue(settings.allowSetSystemTime);
        assertEquals(1000L, settings.commandTimeoutMillis);
        assertFalse(settings.preferTimedatectl);
    }

    @Test
    public void legacyDebugOffsetKeyIsStillSupportedWithMinuteUnit() {
        TimeSettings settings = TimeSettings.fromConfig(new MapConfigView(Map.of(
                "time.debug-offset-ms", 60L
        )));
        assertEquals(3_600_000L, settings.debugOffsetMillis);
    }

    @Test
    public void newDebugOffsetKeyShouldTakePrecedence() {
        TimeSettings settings = TimeSettings.fromConfig(new MapConfigView(Map.of(
                "time.debug-offset-ms", 60L,
                "time.debug-offset-minutes", 120L
        )));
        assertEquals(7_200_000L, settings.debugOffsetMillis);
    }

    @Test
    public void invalidZoneFallsBackToSystemDefault() {
        TimeSettings settings = TimeSettings.fromConfig(new MapConfigView(Map.of(
                "time.zone-id", "not-a-valid-zone"
        )));
        assertEquals(ZoneId.systemDefault(), settings.zoneId);
    }
}
