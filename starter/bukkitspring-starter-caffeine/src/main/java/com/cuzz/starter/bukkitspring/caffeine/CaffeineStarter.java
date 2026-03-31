package com.cuzz.starter.bukkitspring.caffeine;

import com.cuzz.bukkitspring.api.StarterRegistry;
import com.cuzz.starter.bukkitspring.caffeine.autoconfigure.CaffeineDependencies;

import java.util.logging.Logger;

/**
 * Caffeine starter entry.
 *
 * <p>Registers runtime dependencies and scan packages on class load.
 */
public final class CaffeineStarter {
    private static final Logger LOGGER = Logger.getLogger(CaffeineStarter.class.getName());

    static {
        StarterRegistry.registerDependencies(CaffeineDependencies.get());
        StarterRegistry.registerScanPackage("com.cuzz.starter.bukkitspring.caffeine");
        LOGGER.info("[CaffeineStarter] Registered caffeine starter configuration package");
    }

    public CaffeineStarter() {
    }
}
