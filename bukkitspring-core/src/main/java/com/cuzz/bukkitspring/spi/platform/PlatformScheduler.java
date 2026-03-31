package com.cuzz.bukkitspring.spi.platform;

public interface PlatformScheduler {
    void runSync(Runnable task);

    void runAsync(Runnable task);
}
