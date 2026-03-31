package com.cuzz.starter.bukkitspring.kafka.autoconfigure;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Bean;
import com.cuzz.bukkitspring.api.annotation.Configuration;
import com.cuzz.starter.bukkitspring.kafka.config.KafkaConsumerManagerSettings;
import com.cuzz.starter.bukkitspring.kafka.config.KafkaSettings;
import com.cuzz.bukkitspring.spi.config.ConfigView;

import java.util.logging.Logger;

/**
 * Kafka 自动配置类
 * 
 * <p>自动装配 Kafka 服务和消费者管理功能。
 * 配置通过 BukkitSpring 插件的 config.yml 提供。
 * 
 * <p>核心 Bean：
 * <ul>
 *   <li>KafkaSettings - Kafka 配置信息</li>
 *   <li>KafkaConsumerManagerSettings - 消费者管理器配置</li>
 *   <li>KafkaService - 统一的 Kafka 服务接口（全局 Bean）</li>
 * </ul>
 * 
 * <p>使用示例：注入 KafkaService 并使用 registerConsumer 注册消费者，
 * 使用 sendAsync 发送消息。
 */
@Configuration
public class KafkaAutoConfiguration {
    
    /**
     * 创建 KafkaSettings Bean
     * 从 BukkitSpring 插件配置文件中读取 Kafka 配置
     */
    @Bean
    public KafkaSettings kafkaSettings(ConfigView config, Logger logger) {
        KafkaSettings settings = KafkaSettings.fromConfig(config);
        
        if (settings.enabled) {
            logger.info("[Kafka] Enabled with servers: " + settings.bootstrapServers);
        } else {
            logger.info("[Kafka] Disabled");
        }
        
        return settings;
    }
    
    /**
     * 创建 KafkaConsumerManagerSettings Bean
     * 从 KafkaSettings 中提取消费者管理器配置
     */
    @Bean
    public KafkaConsumerManagerSettings kafkaConsumerManagerSettings(KafkaSettings kafkaSettings) {
        return kafkaSettings.consumerManagerSettings;
    }
}
