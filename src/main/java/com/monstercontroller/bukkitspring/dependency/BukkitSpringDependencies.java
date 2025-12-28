package com.monstercontroller.bukkitspring.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BukkitSpringDependencies {
    private BukkitSpringDependencies() {
    }

    public static List<MavenDependency> required() {
        List<MavenDependency> dependencies = new ArrayList<>();
        dependencies.add(new MavenDependency("io.github.classgraph", "classgraph", "4.8.165"));
        dependencies.add(new MavenDependency("jakarta.annotation", "jakarta.annotation-api", "2.1.1"));
        dependencies.add(new MavenDependency("org.slf4j", "slf4j-api", "1.7.36"));
        dependencies.add(new MavenDependency("org.apache.kafka", "kafka-clients", "3.7.0"));
        dependencies.add(new MavenDependency("com.github.luben", "zstd-jni", "1.5.5-6"));
        dependencies.add(new MavenDependency("org.lz4", "lz4-java", "1.8.0"));
        dependencies.add(new MavenDependency("org.xerial.snappy", "snappy-java", "1.1.10.5"));
        dependencies.add(new MavenDependency("redis.clients", "jedis", "5.2.0"));
        dependencies.add(new MavenDependency("org.apache.commons", "commons-pool2", "2.12.0"));
        dependencies.add(new MavenDependency("org.json", "json", "20240303"));
        return Collections.unmodifiableList(dependencies);
    }
}
