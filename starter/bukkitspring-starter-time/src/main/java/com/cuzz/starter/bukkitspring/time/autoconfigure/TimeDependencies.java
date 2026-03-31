package com.cuzz.starter.bukkitspring.time.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;

import java.util.Collections;
import java.util.List;

/**
 * Time starter dependency definitions.
 */
public final class TimeDependencies {

    private TimeDependencies() {
    }

    /**
     * Return all runtime dependencies required by the time starter.
     */
    public static List<MavenDependency> get() {
        return Collections.emptyList();
    }
}
