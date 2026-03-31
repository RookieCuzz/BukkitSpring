package com.cuzz.starter.bukkitspring.caffeine.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;

import java.util.Arrays;
import java.util.List;

/**
 * Caffeine starter dependency definitions.
 */
public final class CaffeineDependencies {

    private CaffeineDependencies() {
    }

    /**
     * Return all runtime dependencies required by the caffeine starter.
     *
     * <p>BukkitSpring does not resolve transitive dependencies automatically, so
     * this list includes runtime transitive dependencies from caffeine 3.1.8.
     */
    public static List<MavenDependency> get() {
        return Arrays.asList(
                new MavenDependency("com.github.ben-manes.caffeine", "caffeine", "3.1.8"),
                new MavenDependency("org.checkerframework", "checker-qual", "3.37.0"),
                new MavenDependency("com.google.errorprone", "error_prone_annotations", "2.21.1")
        );
    }
}
