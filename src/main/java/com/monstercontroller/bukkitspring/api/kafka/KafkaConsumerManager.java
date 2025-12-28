package com.monstercontroller.bukkitspring.api.kafka;

import java.util.List;

public interface KafkaConsumerManager {
    String registerConsumer(ConsumerRegistration<?, ?> registration);

    void startAll();

    void stopAll();

    void start(String consumerId);

    void stop(String consumerId);

    void pause(String consumerId);

    void resume(String consumerId);

    ConsumerStatus getStatus(String consumerId);

    List<ConsumerInfo> listAll();
}
