package com.cuzz.starter.bukkitspring.rocketmq.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;

import java.util.Arrays;
import java.util.List;

/**
 * RocketMQ starter dependency definitions.
 */
public final class RocketMqDependencies {

    private RocketMqDependencies() {
    }

    /**
     * Return all runtime dependencies required by the RocketMQ starter.
     *
     * <p>BukkitSpring does not resolve transitive dependencies automatically, so this
     * list includes dependencies from {@code rocketmq-client:4.9.8} runtime tree.
     */
    public static List<MavenDependency> get() {
        return Arrays.asList(
                new MavenDependency("org.apache.rocketmq", "rocketmq-client", "4.9.8"),
                new MavenDependency("org.apache.rocketmq", "rocketmq-common", "4.9.8"),
                new MavenDependency("org.apache.rocketmq", "rocketmq-remoting", "4.9.8"),
                new MavenDependency("org.apache.rocketmq", "rocketmq-logging", "4.9.8"),
                new MavenDependency("com.alibaba", "fastjson", "1.2.69_noneautotype"),
                new MavenDependency("io.netty", "netty-all", "4.1.65.Final"),
                new MavenDependency("commons-validator", "commons-validator", "1.7"),
                new MavenDependency("commons-beanutils", "commons-beanutils", "1.9.4"),
                new MavenDependency("commons-digester", "commons-digester", "2.1"),
                new MavenDependency("commons-logging", "commons-logging", "1.2"),
                new MavenDependency("commons-collections", "commons-collections", "3.2.2"),
                new MavenDependency("com.github.luben", "zstd-jni", "1.5.2-2"),
                new MavenDependency("org.lz4", "lz4-java", "1.8.0"),
                new MavenDependency("org.apache.commons", "commons-lang3", "3.4")
        );
    }
}
