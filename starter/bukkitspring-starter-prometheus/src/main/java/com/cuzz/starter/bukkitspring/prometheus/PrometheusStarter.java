package com.cuzz.starter.bukkitspring.prometheus;

import com.cuzz.bukkitspring.api.StarterRegistry;
import com.cuzz.starter.bukkitspring.prometheus.autoconfigure.PrometheusDependencies;

import java.util.logging.Logger;

/**
 * Prometheus starter entry.
 *
 * <p>Registers runtime dependencies and scan packages on class load.
 */
public final class PrometheusStarter {
    private static final Logger LOGGER = Logger.getLogger(PrometheusStarter.class.getName());

    static {
        StarterRegistry.registerDependencies(PrometheusDependencies.get());
        StarterRegistry.registerScanPackage("com.cuzz.starter.bukkitspring.prometheus");
        LOGGER.info("[PrometheusStarter] Registered Prometheus dependencies and configuration package");
    }

    public PrometheusStarter() {
    }
}
