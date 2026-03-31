package com.cuzz.starter.bukkitspring.redisson.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;

import java.util.Arrays;
import java.util.List;

/**
 * Redisson starter dependency definitions.
 */
public final class RedissonDependencies {

    private RedissonDependencies() {
    }

    public static List<MavenDependency> get() {
        return Arrays.asList(
                new MavenDependency("org.redisson", "redisson-all", "3.31.0"),
                new MavenDependency("org.slf4j", "slf4j-api", "1.7.36")
        );
    }
}
