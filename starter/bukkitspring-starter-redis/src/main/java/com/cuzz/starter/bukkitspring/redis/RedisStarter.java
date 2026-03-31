package com.cuzz.starter.bukkitspring.redis;

import com.cuzz.bukkitspring.api.StarterRegistry;
import com.cuzz.starter.bukkitspring.redis.autoconfigure.RedisDependencies;
import java.util.logging.Logger;

/**
 * Redis starter entry class.
 *
 * <p>Registers runtime dependencies and scan packages on class load.
 * BukkitSpring loads this class during onLoad() and triggers the static block.
 *
 * <p>MANIFEST.MF entry:
 * <pre>
 * Starter-Class: com.cuzz.starter.bukkitspring.redis.RedisStarter
 * </pre>
 */
public final class RedisStarter {
    private static final Logger LOGGER = Logger.getLogger(RedisStarter.class.getName());

    static {
        StarterRegistry.registerDependencies(RedisDependencies.get());
        StarterRegistry.registerScanPackage("com.cuzz.starter.bukkitspring.redis");
        LOGGER.info("[RedisStarter] Registered Redis dependencies and configuration package");
    }

    public RedisStarter() {
    }
}
