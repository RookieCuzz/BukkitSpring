package com.cuzz.starter.bukkitspring.kafka.config;

import com.cuzz.starter.bukkitspring.kafka.api.ConsumerErrorHandlingStrategy;

/**
 * Kafka 消费者管理器配置
 */
public final class KafkaConsumerManagerSettings {
    public final int shutdownTimeout;
    public final ConsumerErrorHandlingStrategy errorHandlingStrategy;
    public final boolean enableConsumeLogging;
    
    public KafkaConsumerManagerSettings(int shutdownTimeout, 
                                        ConsumerErrorHandlingStrategy errorHandlingStrategy,
                                        boolean enableConsumeLogging) {
        this.shutdownTimeout = shutdownTimeout;
        this.errorHandlingStrategy = errorHandlingStrategy;
        this.enableConsumeLogging = enableConsumeLogging;
    }
}
