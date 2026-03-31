package com.cuzz.bukkitspring.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BukkitSpringDependencies {
    private BukkitSpringDependencies() {
    }

    public static List<MavenDependency> required() {
        List<MavenDependency> dependencies = new ArrayList<>();
        // 核心依赖
        dependencies.add(new MavenDependency("io.github.classgraph", "classgraph", "4.8.165"));

        return Collections.unmodifiableList(dependencies);
    }
}
