package com.cuzz.starter.bukkitspring.time.api;

import com.cuzz.starter.bukkitspring.time.config.TimeSettings;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public interface TimeService extends AutoCloseable {
    boolean isEnabled();

    TimeSettings settings();

    ExecutorService executor();

    Instant now();

    long currentTimeMillis();

    ZonedDateTime now(ZoneId zoneId);

    String formatNow(String pattern, String zoneId);

    Duration debugOffset();

    void setDebugOffset(Duration offset);

    TimeSetResult setSystemTime(Instant target);

    CompletableFuture<TimeSetResult> setSystemTimeAsync(Instant target);

    @Override
    void close();
}
