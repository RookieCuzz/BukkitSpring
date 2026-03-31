package com.cuzz.starter.bukkitspring.time;

import com.cuzz.bukkitspring.api.StarterRegistry;
import com.cuzz.starter.bukkitspring.time.autoconfigure.TimeDependencies;

import java.util.logging.Logger;

/**
 * Time starter entry.
 *
 * <p>Registers runtime dependencies and scan packages on class load.
 */
public final class TimeStarter {
    private static final Logger LOGGER = Logger.getLogger(TimeStarter.class.getName());

    static {
        StarterRegistry.registerDependencies(TimeDependencies.get());
        StarterRegistry.registerScanPackage("com.cuzz.starter.bukkitspring.time");
        LOGGER.info("[TimeStarter] Registered time starter configuration package");
    }

    public TimeStarter() {
    }
}
