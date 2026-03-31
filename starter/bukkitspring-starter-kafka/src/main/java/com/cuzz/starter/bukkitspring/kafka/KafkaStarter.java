package com.cuzz.starter.bukkitspring.kafka;

import com.cuzz.bukkitspring.api.StarterRegistry;
import com.cuzz.starter.bukkitspring.kafka.autoconfigure.KafkaDependencies;
import java.util.logging.Logger;

/**
 * Kafka Starter 入口类
 * 
 * <p>在类加载时自动注册 Kafka 依赖和自动配置包。
 * BukkitSpring 会在 onLoad() 阶段加载此类并触发静态初始化块。
 * 
 * <p>MANIFEST.MF 配置:
 * <pre>
 * Starter-Class: com.cuzz.starter.bukkitspring.kafka.KafkaStarter
 * </pre>
 */
public final class KafkaStarter {
    private static final Logger LOGGER = Logger.getLogger(KafkaStarter.class.getName());
    
    static {
        // 注册 Kafka 依赖（运行时下载）
        StarterRegistry.registerDependencies(KafkaDependencies.get());
        
        // 注册自动配置包（扫描 @Configuration）
        StarterRegistry.registerScanPackage("com.cuzz.starter.bukkitspring.kafka");
        
        LOGGER.info("[KafkaStarter] Registered Kafka dependencies and configuration package");
    }
    
    public KafkaStarter() {
    }
}
