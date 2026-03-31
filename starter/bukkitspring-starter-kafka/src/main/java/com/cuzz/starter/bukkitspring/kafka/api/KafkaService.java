package com.cuzz.starter.bukkitspring.kafka.api;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Kafka 服务接口
 * 提供 Kafka 消息的发送和消费功能
 */
public interface KafkaService {
    boolean isEnabled();

    ExecutorService executor();

    Map<String, Object> baseProperties();

    <K, V> KafkaProducer<K, V> createProducer(Map<String, Object> overrides);

    <K, V> KafkaConsumer<K, V> createConsumer(String groupId, Map<String, Object> overrides);

    Admin createAdmin(Map<String, Object> overrides);

    default CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor());
    }

    default <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor());
    }

    /**
     * 发送消息到指定的 topic（同步）
     * @param topic topic 名称
     * @param key 消息的 key
     * @param value 消息的 value
     * @return RecordMetadata
     */
    default <K, V> RecordMetadata send(String topic, K key, V value) {
        try (KafkaProducer<K, V> producer = createProducer(Map.of(
                "key.serializer", "org.apache.kafka.common.serialization.StringSerializer",
                "value.serializer", "org.apache.kafka.common.serialization.StringSerializer"
        ))) {
            ProducerRecord<K, V> record = new ProducerRecord<>(topic, key, value);
            return producer.send(record).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message to topic: " + topic, e);
        }
    }

    /**
     * 发送消息到指定的 topic（同步，无 key）
     * @param topic topic 名称
     * @param value 消息的 value
     * @return RecordMetadata
     */
    default <V> RecordMetadata send(String topic, V value) {
        return send(topic, null, value);
    }

    /**
     * 异步发送消息到指定的 topic
     * @param topic topic 名称
     * @param key 消息的 key
     * @param value 消息的 value
     * @return CompletableFuture<RecordMetadata>
     */
    default <K, V> CompletableFuture<RecordMetadata> sendAsync(String topic, K key, V value) {
        return supplyAsync(() -> send(topic, key, value));
    }

    /**
     * 异步发送消息到指定的 topic（无 key）
     * @param topic topic 名称
     * @param value 消息的 value
     * @return CompletableFuture<RecordMetadata>
     */
    default <V> CompletableFuture<RecordMetadata> sendAsync(String topic, V value) {
        return sendAsync(topic, null, value);
    }

    /**
     * 使用自定义序列化器发送消息
     * @param topic topic 名称
     * @param key 消息的 key
     * @param value 消息的 value
     * @param keySerializer key 序列化器类名
     * @param valueSerializer value 序列化器类名
     * @return RecordMetadata
     */
    default <K, V> RecordMetadata sendWithSerializer(String topic, K key, V value, 
                                                      String keySerializer, String valueSerializer) {
        try (KafkaProducer<K, V> producer = createProducer(Map.of(
                "key.serializer", keySerializer,
                "value.serializer", valueSerializer
        ))) {
            ProducerRecord<K, V> record = new ProducerRecord<>(topic, key, value);
            return producer.send(record).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message to topic: " + topic, e);
        }
    }

    /**
     * 异步发送消息（使用自定义序列化器）
     * @param topic topic 名称
     * @param key 消息的 key
     * @param value 消息的 value
     * @param keySerializer key 序列化器类名
     * @param valueSerializer value 序列化器类名
     * @return CompletableFuture<RecordMetadata>
     */
    default <K, V> CompletableFuture<RecordMetadata> sendAsyncWithSerializer(String topic, K key, V value,
                                                                              String keySerializer, String valueSerializer) {
        return supplyAsync(() -> sendWithSerializer(topic, key, value, keySerializer, valueSerializer));
    }
    
    // ==================== 消费者管理功能 ====================
    
    /**
     * 注册消费者
     * 
     * @param registration 消费者注册信息
     * @param <K> 消息 key 类型
     * @param <V> 消息 value 类型
     * @return 消费者唯一标识 ID
     */
    <K, V> String registerConsumer(ConsumerRegistration<K, V> registration);
    
    /**
     * 启动指定消费者
     * 
     * @param consumerId 消费者 ID
     */
    void startConsumer(String consumerId);
    
    /**
     * 停止指定消费者
     * 
     * @param consumerId 消费者 ID
     */
    void stopConsumer(String consumerId);
    
    /**
     * 暂停指定消费者
     * 
     * @param consumerId 消费者 ID
     */
    void pauseConsumer(String consumerId);
    
    /**
     * 恢复指定消费者
     * 
     * @param consumerId 消费者 ID
     */
    void resumeConsumer(String consumerId);
    
    /**
     * 获取消费者状态
     * 
     * @param consumerId 消费者 ID
     * @return 消费者状态，如果不存在返回 null
     */
    ConsumerStatus getConsumerStatus(String consumerId);
    
    /**
     * 列出所有消费者信息
     * 
     * @return 消费者信息列表
     */
    List<ConsumerInfo> listConsumers();
    
    /**
     * 启动所有自动启动的消费者
     * 通常在插件启用时调用
     */
    void startAllConsumers();
    
    /**
     * 关闭 Kafka 服务
     * 释放所有资源，通常在插件禁用时调用
     */
    void close();
}