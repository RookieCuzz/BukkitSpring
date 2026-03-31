package com.cuzz.starter.bukkitspring.time.config;

import com.cuzz.bukkitspring.spi.config.ConfigSection;
import com.cuzz.bukkitspring.spi.config.ConfigView;

import java.time.ZoneId;

/**
 * Time starter settings.
 */
public final class TimeSettings {
    private static final long MAX_DEBUG_OFFSET_MINUTES = 5_259_600L; // 10 years

    public final boolean enabled;
    public final boolean useVirtualThreads;
    public final ZoneId zoneId;
    public final long debugOffsetMillis;
    public final boolean allowSetSystemTime;
    public final long commandTimeoutMillis;
    public final boolean preferTimedatectl;

    private TimeSettings(boolean enabled,
                         boolean useVirtualThreads,
                         ZoneId zoneId,
                         long debugOffsetMillis,
                         boolean allowSetSystemTime,
                         long commandTimeoutMillis,
                         boolean preferTimedatectl) {
        this.enabled = enabled;
        this.useVirtualThreads = useVirtualThreads;
        this.zoneId = zoneId;
        this.debugOffsetMillis = debugOffsetMillis;
        this.allowSetSystemTime = allowSetSystemTime;
        this.commandTimeoutMillis = commandTimeoutMillis;
        this.preferTimedatectl = preferTimedatectl;
    }

    public static TimeSettings fromConfig(ConfigView config) {
        ConfigView safeConfig = config == null ? EmptyConfigView.INSTANCE : config;

        boolean enabled = safeConfig.getBoolean("time.enabled", false);
        boolean useVirtualThreads = safeConfig.getBoolean("time.virtual-threads", true);
        ZoneId zoneId = parseZoneId(safeConfig.getString("time.zone-id", ZoneId.systemDefault().getId()));
        long configuredOffsetMinutes = safeConfig.getLong("time.debug-offset-minutes", Long.MIN_VALUE);
        if (configuredOffsetMinutes == Long.MIN_VALUE) {
            // Backward-compatible key: value is interpreted as minutes as well.
            configuredOffsetMinutes = safeConfig.getLong("time.debug-offset-ms", 0L);
        }
        long debugOffsetMinutes = clampLong(configuredOffsetMinutes, -MAX_DEBUG_OFFSET_MINUTES, MAX_DEBUG_OFFSET_MINUTES);
        long debugOffsetMillis = debugOffsetMinutes * 60_000L;

        boolean allowSetSystemTime = safeConfig.getBoolean("time.system-time.allow-set", false);
        long commandTimeoutMillis = clampLong(safeConfig.getLong("time.system-time.command-timeout-ms", 5000L), 1000L, 120000L);
        boolean preferTimedatectl = safeConfig.getBoolean("time.system-time.prefer-timedatectl", true);

        return new TimeSettings(
                enabled,
                useVirtualThreads,
                zoneId,
                debugOffsetMillis,
                allowSetSystemTime,
                commandTimeoutMillis,
                preferTimedatectl
        );
    }

    private static ZoneId parseZoneId(String value) {
        if (value == null || value.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(value.trim());
        } catch (Exception ignored) {
            return ZoneId.systemDefault();
        }
    }

    private static long clampLong(long value, long min, long max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private enum EmptyConfigView implements ConfigView {
        INSTANCE;

        @Override
        public boolean getBoolean(String path, boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public String getString(String path, String defaultValue) {
            return defaultValue;
        }

        @Override
        public int getInt(String path, int defaultValue) {
            return defaultValue;
        }

        @Override
        public long getLong(String path, long defaultValue) {
            return defaultValue;
        }

        @Override
        public ConfigSection getSection(String path) {
            return null;
        }
    }
}
