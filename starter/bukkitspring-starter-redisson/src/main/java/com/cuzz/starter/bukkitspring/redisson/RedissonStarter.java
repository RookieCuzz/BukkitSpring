package com.cuzz.starter.bukkitspring.redisson;

import com.cuzz.bukkitspring.api.StarterRegistry;
import com.cuzz.starter.bukkitspring.redisson.autoconfigure.RedissonDependencies;

import java.util.logging.Logger;

/**
 * Redisson starter entry class.
 */
public final class RedissonStarter {
    private static final Logger LOGGER = Logger.getLogger(RedissonStarter.class.getName());

    static {
        StarterRegistry.registerDependencies(RedissonDependencies.get());
        StarterRegistry.registerScanPackage("com.cuzz.starter.bukkitspring.redisson");
        LOGGER.info("[RedissonStarter] Registered Redisson dependencies and configuration package");
    }

    public RedissonStarter() {
    }
}
