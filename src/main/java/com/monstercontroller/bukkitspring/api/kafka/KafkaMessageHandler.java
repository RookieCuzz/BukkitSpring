package com.monstercontroller.bukkitspring.api.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

@FunctionalInterface
public interface KafkaMessageHandler<K, V> {
    void handle(ConsumerRecord<K, V> record) throws Exception;
}
