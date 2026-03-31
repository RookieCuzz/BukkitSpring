package com.cuzz.starter.bukkitspring.config;

import com.cuzz.bukkitspring.api.StarterRegistry;
import com.cuzz.starter.bukkitspring.config.autoconfigure.ConfigDependencies;

import java.util.logging.Logger;

/**
 * Config starter entry.
 */
public final class ConfigStarter {
    private static final Logger LOGGER = Logger.getLogger(ConfigStarter.class.getName());

    static {
        StarterRegistry.registerDependencies(ConfigDependencies.get());
        StarterRegistry.registerScanPackage("com.cuzz.starter.bukkitspring.config");
        LOGGER.info("[ConfigStarter] Registered config starter configuration package");
    }

    public ConfigStarter() {
    }
}
