package com.cuzz.bukkitspring.spi.starter;

public interface StarterLifecycle {
    void initialize(StarterContext context);

    void cleanup();
}
