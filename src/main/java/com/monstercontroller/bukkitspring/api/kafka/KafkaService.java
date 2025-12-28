package com.monstercontroller.bukkitspring.api.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

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
}
