package com.cuzz.starter.bukkitspring.redis.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;

import java.util.Arrays;
import java.util.List;

/**
 * Redis starter dependency definitions.
 */
public final class RedisDependencies {

    private RedisDependencies() {
    }

    /**
     * Return all runtime dependencies required by the Redis starter.
     */
    public static List<MavenDependency> get() {
        return Arrays.asList(
                new MavenDependency("redis.clients", "jedis", "5.2.0"),
                new MavenDependency("org.apache.commons", "commons-pool2", "2.12.0"),
                new MavenDependency("org.slf4j", "slf4j-api", "1.7.36")
        );
    }
}
