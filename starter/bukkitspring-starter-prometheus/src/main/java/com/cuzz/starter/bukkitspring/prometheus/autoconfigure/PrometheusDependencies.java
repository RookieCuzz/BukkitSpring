package com.cuzz.starter.bukkitspring.prometheus.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;

import java.util.Arrays;
import java.util.List;

/**
 * Prometheus starter dependency definitions.
 */
public final class PrometheusDependencies {

    private PrometheusDependencies() {
    }

    /**
     * Return all runtime dependencies required by the Prometheus starter.
     */
    public static List<MavenDependency> get() {
        return Arrays.asList(
                new MavenDependency("io.prometheus", "simpleclient", "0.16.0"),
                new MavenDependency("io.prometheus", "simpleclient_common", "0.16.0"),
                new MavenDependency("io.prometheus", "simpleclient_hotspot", "0.16.0"),
                new MavenDependency("io.prometheus", "simpleclient_pushgateway", "0.16.0"),
                new MavenDependency("io.prometheus", "simpleclient_tracer_common", "0.16.0"),
                new MavenDependency("io.prometheus", "simpleclient_tracer_otel", "0.16.0"),
                new MavenDependency("io.prometheus", "simpleclient_tracer_otel_agent", "0.16.0")
        );
    }
}
