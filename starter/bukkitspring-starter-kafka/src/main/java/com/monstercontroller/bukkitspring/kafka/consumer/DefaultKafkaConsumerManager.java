package com.monstercontroller.bukkitspring.kafka;

import com.monstercontroller.bukkitspring.api.kafka.ConsumerInfo;
import com.monstercontroller.bukkitspring.api.kafka.ConsumerRegistration;
import com.monstercontroller.bukkitspring.api.kafka.ConsumerStatus;
import com.monstercontroller.bukkitspring.api.kafka.KafkaConsumerManager;
import com.monstercontroller.bukkitspring.api.kafka.KafkaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class DefaultKafkaConsumerManager implements KafkaConsumerManager {
    private final KafkaService kafkaService;
    private final KafkaConsumerManagerSettings settings;
    private final Logger logger;
    private final boolean preferVirtualThreads;
    private final ConcurrentMap<String, ManagedConsumer<?, ?>> consumers = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(1);

    public DefaultKafkaConsumerManager(KafkaService kafkaService, KafkaSettings kafkaSettings, Logger logger) {
        this.kafkaService = Objects.requireNonNull(kafkaService, "kafkaService");
        this.settings = Objects.requireNonNull(kafkaSettings, "kafkaSettings").consumerManagerSettings;
        this.logger = Objects.requireNonNull(logger, "logger");
        this.preferVirtualThreads = kafkaSettings.useVirtualThreads;
    }

    @Override
    public String registerConsumer(ConsumerRegistration<?, ?> registration) {
        Objects.requireNonNull(registration, "registration");
        String id = registration.id == null || registration.id.isBlank()
                ? "consumer-" + idSequence.getAndIncrement()
                : registration.id;
        if (consumers.containsKey(id)) {
            throw new IllegalArgumentException("Consumer already registered: " + id);
        }
        ManagedConsumer<?, ?> managed = new ManagedConsumer<>(
                id,
                registration,
                kafkaService,
                settings,
                logger,
                preferVirtualThreads
        );
        consumers.put(id, managed);
        if (registration.autoStartup) {
            managed.start();
        }
        return id;
    }

    @Override
    public void startAll() {
        for (ManagedConsumer<?, ?> consumer : consumers.values()) {
            if (consumer.isAutoStartup()) {
                consumer.start();
            }
        }
    }

    @Override
    public void stopAll() {
        for (ManagedConsumer<?, ?> consumer : consumers.values()) {
            consumer.stop();
        }
    }

    @Override
    public void start(String consumerId) {
        ManagedConsumer<?, ?> consumer = consumers.get(consumerId);
        if (consumer == null) {
            logger.warning("Kafka consumer not found: " + consumerId);
            return;
        }
        consumer.start();
    }

    @Override
    public void stop(String consumerId) {
        ManagedConsumer<?, ?> consumer = consumers.get(consumerId);
        if (consumer == null) {
            logger.warning("Kafka consumer not found: " + consumerId);
            return;
        }
        consumer.stop();
    }

    @Override
    public void pause(String consumerId) {
        ManagedConsumer<?, ?> consumer = consumers.get(consumerId);
        if (consumer == null) {
            logger.warning("Kafka consumer not found: " + consumerId);
            return;
        }
        consumer.pause();
    }

    @Override
    public void resume(String consumerId) {
        ManagedConsumer<?, ?> consumer = consumers.get(consumerId);
        if (consumer == null) {
            logger.warning("Kafka consumer not found: " + consumerId);
            return;
        }
        consumer.resume();
    }

    @Override
    public ConsumerStatus getStatus(String consumerId) {
        ManagedConsumer<?, ?> consumer = consumers.get(consumerId);
        if (consumer == null) {
            return null;
        }
        return consumer.status();
    }

    @Override
    public List<ConsumerInfo> listAll() {
        List<ConsumerInfo> list = new ArrayList<>();
        for (ManagedConsumer<?, ?> consumer : consumers.values()) {
            list.add(consumer.info());
        }
        return list;
    }
}
