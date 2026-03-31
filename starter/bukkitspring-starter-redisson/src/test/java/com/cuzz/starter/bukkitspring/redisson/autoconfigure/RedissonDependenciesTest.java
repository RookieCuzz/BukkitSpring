package com.cuzz.starter.bukkitspring.redisson.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RedissonDependenciesTest {

    @Test
    public void shouldContainRedissonRuntimeDependencies() {
        List<MavenDependency> dependencies = RedissonDependencies.get();
        assertTrue(contains(dependencies, "org.redisson", "redisson-all"));
        assertTrue(contains(dependencies, "org.slf4j", "slf4j-api"));
    }

    private static boolean contains(List<MavenDependency> dependencies, String groupId, String artifactId) {
        return dependencies.stream().anyMatch(dep ->
                groupId.equals(dep.groupId()) && artifactId.equals(dep.artifactId()));
    }
}
