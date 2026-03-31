package com.cuzz.starter.bukkitspring.time.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeDependenciesTest {

    @Test
    public void noExtraDependenciesRequired() {
        assertTrue(TimeDependencies.get().isEmpty());
    }
}
