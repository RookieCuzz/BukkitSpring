package com.cuzz.starter.bukkitspring.kafka.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;

import java.util.Arrays;
import java.util.List;

/**
 * Kafka Starter 依赖定义
 */
public final class KafkaDependencies {
    
    private KafkaDependencies() {
    }
    
    /**
     * 获取 Kafka Starter 需要的所有依赖
     * 
     * @return 依赖列表
     */
    public static List<MavenDependency> get() {
        return Arrays.asList(
            // Kafka 客户端
            new MavenDependency("org.apache.kafka", "kafka-clients", "3.7.0"),
            // Kafka 压缩库依赖
            new MavenDependency("com.github.luben", "zstd-jni", "1.5.5-6"),
            new MavenDependency("org.lz4", "lz4-java", "1.8.0"),
            new MavenDependency("org.xerial.snappy", "snappy-java", "1.1.10.5")
        );
    }
}
