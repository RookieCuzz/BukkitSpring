package com.monstercontroller.bukkitspring.kafka;

import com.monstercontroller.bukkitspring.api.kafka.ConsumerErrorHandlingStrategy;
import com.monstercontroller.bukkitspring.api.kafka.ConsumerInfo;
import com.monstercontroller.bukkitspring.api.kafka.ConsumerRegistration;
import com.monstercontroller.bukkitspring.api.kafka.ConsumerState;
import com.monstercontroller.bukkitspring.api.kafka.ConsumerStatus;
import com.monstercontroller.bukkitspring.api.kafka.KafkaMessageHandler;
import com.monstercontroller.bukkitspring.api.kafka.KafkaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ManagedConsumer<K, V> {
    private static final Duration POLL_DURATION = Duration.ofMillis(100);

    private final String id;
    private final List<String> topics;
    private final String groupId;
    private final KafkaMessageHandler<K, V> handler;
    private final int concurrency;
    private final boolean autoStartup;
    private final Map<String, Object> properties;
    private final KafkaService kafkaService;
    private final Logger logger;
    private final ConsumerErrorHandlingStrategy errorHandlingStrategy;
    private final boolean enableConsumeLogging;
    private final int shutdownTimeoutSeconds;
    private final boolean preferVirtualThreads;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicLong messagesConsumed = new AtomicLong(0);
    private final AtomicLong lastPollTime = new AtomicLong(0);
    private final AtomicReference<ConsumerState> state = new AtomicReference<>(ConsumerState.STOPPED);
    private final ConcurrentMap<TopicPartition, Long> currentOffsets = new ConcurrentHashMap<>();
    private final List<ConsumerWorker> workers = new CopyOnWriteArrayList<>();
    private volatile ExecutorService executor;

    ManagedConsumer(String id,
                    ConsumerRegistration<K, V> registration,
                    KafkaService kafkaService,
                    KafkaConsumerManagerSettings settings,
                    Logger logger,
                    boolean preferVirtualThreads) {
        this.id = Objects.requireNonNull(id, "id");
        this.topics = registration.topics;
        this.groupId = registration.groupId;
        this.handler = registration.handler;
        this.concurrency = registration.concurrency;
        this.autoStartup = registration.autoStartup;
        this.properties = registration.properties;
        this.kafkaService = Objects.requireNonNull(kafkaService, "kafkaService");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.errorHandlingStrategy = settings.errorHandlingStrategy;
        this.enableConsumeLogging = settings.enableConsumeLogging;
        this.shutdownTimeoutSeconds = settings.shutdownTimeoutSeconds;
        this.preferVirtualThreads = preferVirtualThreads;
    }

    String getId() {
        return id;
    }

    boolean isAutoStartup() {
        return autoStartup;
    }

    ConsumerStatus status() {
        return new ConsumerStatus(id, state.get(), messagesConsumed.get(), lastPollTime.get());
    }

    ConsumerInfo info() {
        return new ConsumerInfo(
                id,
                topics,
                groupId,
                state.get(),
                messagesConsumed.get(),
                lastPollTime.get(),
                Collections.unmodifiableMap(new ConcurrentHashMap<>(currentOffsets))
        );
    }

    void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        if (!kafkaService.isEnabled()) {
            running.set(false);
            logger.warning("Kafka service is disabled. Consumer " + id + " not started.");
            return;
        }
        state.set(ConsumerState.RUNNING);
        paused.set(false);
        executor = KafkaExecutors.create(preferVirtualThreads, logger, "bukkitspring-kafka-consumer-" + id + "-", concurrency);
        workers.clear();
        for (int i = 0; i < concurrency; i++) {
            KafkaConsumer<K, V> consumer = kafkaService.createConsumer(groupId, properties);
            consumer.subscribe(topics);
            ConsumerWorker worker = new ConsumerWorker(i + 1, consumer);
            workers.add(worker);
            executor.execute(worker);
        }
    }

    void stop() {
        requestStop();
        ExecutorService currentExecutor = executor;
        if (currentExecutor == null) {
            return;
        }
        currentExecutor.shutdown();
        try {
            if (!currentExecutor.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                currentExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            currentExecutor.shutdownNow();
        }
    }

    void pause() {
        if (!running.get()) {
            return;
        }
        paused.set(true);
        state.set(ConsumerState.PAUSED);
    }

    void resume() {
        if (!running.get()) {
            return;
        }
        paused.set(false);
        state.set(ConsumerState.RUNNING);
    }

    private void requestStop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        state.set(ConsumerState.STOPPED);
        for (ConsumerWorker worker : workers) {
            worker.wakeup();
        }
    }

    private void processRecord(ConsumerRecord<K, V> record) {
        try {
            if (enableConsumeLogging) {
                logger.info("Kafka record consumed id=" + id + " topic=" + record.topic() + " offset=" + record.offset());
            }
            handler.handle(record);
            messagesConsumed.incrementAndGet();
            currentOffsets.put(new TopicPartition(record.topic(), record.partition()), record.offset());
        } catch (Exception ex) {
            handleConsumerException(record, ex);
        }
    }

    private void handleConsumerException(ConsumerRecord<K, V> record, Exception ex) {
        logger.log(Level.SEVERE, "Kafka consumer error id=" + id
                + " topic=" + record.topic()
                + " partition=" + record.partition()
                + " offset=" + record.offset(), ex);
        switch (errorHandlingStrategy) {
            case STOP:
                requestStop();
                break;
            case RETRY:
            case DLQ:
                logger.warning("Kafka consumer strategy " + errorHandlingStrategy + " not implemented in MVP. Skipping record.");
                break;
            case SKIP:
            default:
                break;
        }
    }

    private final class ConsumerWorker implements Runnable {
        private final int index;
        private final KafkaConsumer<K, V> consumer;
        private boolean pausedApplied;

        private ConsumerWorker(int index, KafkaConsumer<K, V> consumer) {
            this.index = index;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            try {
                pollLoop();
            } finally {
                try {
                    consumer.close();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Kafka consumer close failed id=" + id + " worker=" + index, ex);
                }
            }
        }

        private void pollLoop() {
            while (running.get()) {
                try {
                    ConsumerRecords<K, V> records = consumer.poll(POLL_DURATION);
                    lastPollTime.set(System.currentTimeMillis());
                    applyPauseState();
                    if (paused.get()) {
                        continue;
                    }
                    for (ConsumerRecord<K, V> record : records) {
                        processRecord(record);
                    }
                } catch (WakeupException ex) {
                    if (running.get()) {
                        logger.warning("Kafka consumer wakeup while running id=" + id + " worker=" + index);
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Kafka consumer poll error id=" + id + " worker=" + index, ex);
                    sleepBackoff();
                }
            }
        }

        private void applyPauseState() {
            boolean pausedNow = paused.get();
            if (pausedNow && !pausedApplied) {
                if (!consumer.assignment().isEmpty()) {
                    consumer.pause(consumer.assignment());
                    pausedApplied = true;
                }
            } else if (!pausedNow && pausedApplied) {
                if (!consumer.assignment().isEmpty()) {
                    consumer.resume(consumer.assignment());
                }
                pausedApplied = false;
            }
        }

        private void wakeup() {
            try {
                consumer.wakeup();
            } catch (Exception ignored) {
                // ignore
            }
        }

        private void sleepBackoff() {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
