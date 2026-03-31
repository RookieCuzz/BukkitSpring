package com.cuzz.starter.bukkitspring.caffeine.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaffeineDependenciesTest {

    @Test
    public void containsRequiredDependencies() {
        List<MavenDependency> dependencies = CaffeineDependencies.get();

        assertFalse(dependencies.isEmpty());
        assertTrue(contains(dependencies, "com.github.ben-manes.caffeine", "caffeine", "3.1.8"));
        assertTrue(contains(dependencies, "org.checkerframework", "checker-qual", "3.37.0"));
        assertTrue(contains(dependencies, "com.google.errorprone", "error_prone_annotations", "2.21.1"));
    }

    @Test
    public void hasNoDuplicateCoordinates() {
        List<MavenDependency> dependencies = CaffeineDependencies.get();
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
