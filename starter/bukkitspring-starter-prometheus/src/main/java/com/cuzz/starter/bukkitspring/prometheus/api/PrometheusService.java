package com.cuzz.starter.bukkitspring.prometheus.api;

import io.prometheus.client.CollectorRegistry;

public interface PrometheusService extends AutoCloseable {
    boolean isEnabled();

    CollectorRegistry registry();

    void pushOnce();

    @Override
    void close();
}
