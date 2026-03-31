package com.cuzz.starter.bukkitspring.rocketmq.api;

import com.cuzz.starter.bukkitspring.rocketmq.config.RocketMqSettings;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * RocketMQ facade service.
 */
public interface RocketMqService extends AutoCloseable {
    boolean isEnabled();

    ExecutorService executor();

    RocketMqSettings settings();

    DefaultMQProducer defaultProducer();

    DefaultMQProducer createProducer(Map<String, Object> overrides);

    DefaultMQPushConsumer createPushConsumer(String consumerGroup, Map<String, Object> overrides);

    DefaultMQPushConsumer subscribeConcurrently(String consumerGroup,
                                                String topic,
                                                String subExpression,
                                                MessageListenerConcurrently listener,
                                                Map<String, Object> overrides);

    SendResult send(Message message);

    SendResult send(String topic, String body);

    SendResult send(String topic, String tags, String body);

    CompletableFuture<SendResult> sendAsync(Message message);

    CompletableFuture<SendResult> sendAsync(String topic, String body);

    CompletableFuture<SendResult> sendAsync(String topic, String tags, String body);

    default CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor());
    }

    default <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor());
    }

    @Override
    void close();
}
