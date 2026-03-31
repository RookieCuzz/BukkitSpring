package com.cuzz.starter.bukkitspring.caffeine.api;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

/**
 * Caffeine cache build spec.
 */
public final class CaffeineCacheSpec {
    public final int initialCapacity;
    public final long maximumSize;
    public final long expireAfterWriteMillis;
    public final long expireAfterAccessMillis;
    public final long refreshAfterWriteMillis;
    public final boolean weakKeys;
    public final boolean weakValues;
    public final boolean softValues;
    public final boolean recordStats;

    public CaffeineCacheSpec(int initialCapacity,
                             long maximumSize,
                             long expireAfterWriteMillis,
                             long expireAfterAccessMillis,
                             long refreshAfterWriteMillis,
                             boolean weakKeys,
                             boolean weakValues,
                             boolean softValues,
                             boolean recordStats) {
        this.initialCapacity = normalizeInt(initialCapacity);
        this.maximumSize = normalizeLong(maximumSize);
        this.expireAfterWriteMillis = normalizeLong(expireAfterWriteMillis);
        this.expireAfterAccessMillis = normalizeLong(expireAfterAccessMillis);
        this.refreshAfterWriteMillis = normalizeLong(refreshAfterWriteMillis);
        this.weakKeys = weakKeys;
        this.weakValues = weakValues;
        this.softValues = !weakValues && softValues;
        this.recordStats = recordStats;
    }

    public static CaffeineCacheSpec defaults() {
        return new CaffeineCacheSpec(
                64,
                10_000L,
                0L,
                0L,
                0L,
                false,
                false,
                false,
                false
        );
    }

    public Caffeine<Object, Object> newBuilder() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (initialCapacity > 0) {
            builder.initialCapacity(initialCapacity);
        }
        if (maximumSize > 0) {
            builder.maximumSize(maximumSize);
        }
        if (expireAfterWriteMillis > 0) {
            builder.expireAfterWrite(Duration.ofMillis(expireAfterWriteMillis));
        }
        if (expireAfterAccessMillis > 0) {
            builder.expireAfterAccess(Duration.ofMillis(expireAfterAccessMillis));
        }
        if (refreshAfterWriteMillis > 0) {
            builder.refreshAfterWrite(Duration.ofMillis(refreshAfterWriteMillis));
        }
        if (weakKeys) {
            builder.weakKeys();
        }
        if (weakValues) {
            builder.weakValues();
        } else if (softValues) {
            builder.softValues();
        }
        if (recordStats) {
            builder.recordStats();
        }
        return builder;
    }

    private static int normalizeInt(int value) {
        return Math.max(0, value);
    }

    private static long normalizeLong(long value) {
        return Math.max(0L, value);
    }
}
