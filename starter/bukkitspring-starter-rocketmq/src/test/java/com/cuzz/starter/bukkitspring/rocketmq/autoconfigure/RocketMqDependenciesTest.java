package com.cuzz.starter.bukkitspring.rocketmq.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RocketMqDependenciesTest {

    @Test
    public void containsRequiredDependencies() {
        List<MavenDependency> dependencies = RocketMqDependencies.get();

        assertFalse(dependencies.isEmpty());
        assertTrue(contains(dependencies, "org.apache.rocketmq", "rocketmq-client", "4.9.8"));
        assertTrue(contains(dependencies, "org.apache.rocketmq", "rocketmq-common", "4.9.8"));
        assertTrue(contains(dependencies, "org.apache.rocketmq", "rocketmq-remoting", "4.9.8"));
        assertTrue(contains(dependencies, "org.apache.rocketmq", "rocketmq-logging", "4.9.8"));
        assertTrue(contains(dependencies, "io.netty", "netty-all", "4.1.65.Final"));
        assertTrue(contains(dependencies, "com.alibaba", "fastjson", "1.2.69_noneautotype"));
        assertTrue(contains(dependencies, "commons-validator", "commons-validator", "1.7"));
        assertTrue(contains(dependencies, "org.apache.commons", "commons-lang3", "3.4"));
    }

    @Test
    public void hasNoDuplicateCoordinates() {
        List<MavenDependency> dependencies = RocketMqDependencies.get();
        Set<String> coordinates = dependencies.stream()
                .map(item -> item.groupId() + ":" + item.artifactId() + ":" + item.version())
                .collect(Collectors.toSet());

        assertTrue(coordinates.size() == dependencies.size());
    }

    private static boolean contains(List<MavenDependency> dependencies,
                                    String groupId,
                                    String artifactId,
                                    String version) {
        return dependencies.stream().anyMatch(item ->
                groupId.equals(item.groupId())
                        && artifactId.equals(item.artifactId())
                        && version.equals(item.version())
        );
    }
}
