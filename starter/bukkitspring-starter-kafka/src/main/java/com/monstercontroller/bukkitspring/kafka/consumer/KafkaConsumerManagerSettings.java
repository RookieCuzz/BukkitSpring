package com.monstercontroller.bukkitspring.kafka;

import com.monstercontroller.bukkitspring.api.kafka.ConsumerErrorHandlingStrategy;

public final class KafkaConsumerManagerSettings {
    public final int shutdownTimeoutSeconds;
    public final ConsumerErrorHandlingStrategy errorHandlingStrategy;
    public final boolean enableConsumeLogging;

    public KafkaConsumerManagerSettings(int shutdownTimeoutSeconds,
                                        ConsumerErrorHandlingStrategy errorHandlingStrategy,
                                        boolean enableConsumeLogging) {
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
        this.errorHandlingStrategy = errorHandlingStrategy;
        this.enableConsumeLogging = enableConsumeLogging;
    }
}
