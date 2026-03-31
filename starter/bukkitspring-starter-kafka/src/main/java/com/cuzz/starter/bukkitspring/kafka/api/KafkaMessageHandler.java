package com.cuzz.starter.bukkitspring.kafka.api;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Kafka 消息处理器接口
 * 
 * @param <K> 消息 key 的类型
 * @param <V> 消息 value 的类型
 */
@FunctionalInterface
public interface KafkaMessageHandler<K, V> {
    
    /**
     * 处理单条消息
     *
     * @param record 消费到的消息记录
     * @throws Exception 处理异常
     */
    void handle(ConsumerRecord<K, V> record) throws Exception;
}
