package com.cuzz.starter.bukkitspring.kafka.internal.consumer;

import com.cuzz.starter.bukkitspring.kafka.api.ConsumerErrorHandlingStrategy;
import com.cuzz.starter.bukkitspring.kafka.api.ConsumerState;
import com.cuzz.starter.bukkitspring.kafka.api.KafkaMessageHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ManagedConsumer<K, V> {

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(100);
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(10);
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;

    private final String id;
    private final KafkaConsumer<K, V> consumer;
    private final List<String> topics;
    private final String groupId;
    private final KafkaMessageHandler<K, V> handler;
    private final Logger logger;
    private final ConsumerErrorHandlingStrategy errorHandlingStrategy;
    private final boolean enableConsumeLogging;

    private volatile ConsumerState state = ConsumerState.STOPPED;
    private volatile boolean running = false;
    private final AtomicLong messagesConsumed = new AtomicLong(0);
    private volatile long lastPollTime = 0;
    private volatile Thread pollThread;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public ManagedConsumer(String id,
                           KafkaConsumer<K, V> consumer,
                           List<String> topics,
                           String groupId,
                           KafkaMessageHandler<K, V> handler,
                           Logger logger,
                           ConsumerErrorHandlingStrategy errorHandlingStrategy,
                           boolean enableConsumeLogging) {
        this.id = Objects.requireNonNull(id, "id");
        this.consumer = Objects.requireNonNull(consumer, "consumer");
        this.topics = Objects.requireNonNull(topics, "topics");
        this.groupId = Objects.requireNonNull(groupId, "groupId");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.errorHandlingStrategy = Objects.requireNonNull(errorHandlingStrategy, "errorHandlingStrategy");
        this.enableConsumeLogging = enableConsumeLogging;
    }

    public String getId() {
        return id;
    }

    public List<String> getTopics() {
        return topics;
    }

    public String getGroupId() {
        return groupId;
    }

    public ConsumerState getState() {
        return state;
    }

    public long getMessagesConsumed() {
        return messagesConsumed.get();
    }

    public long getLastPollTime() {
        return lastPollTime;
    }

    public int getConcurrency() {
        return 1;
    }

    public synchronized void start() {
        if (running) {
            logger.warning("Consumer [" + id + "] already running");
            return;
        }

        state = ConsumerState.STARTING;
        running = true;

        try {
            consumer.subscribe(topics);
            logger.info(String.format("Consumer [%s] subscribed topics=%s groupId=%s", id, topics, groupId));
            startPollThread();
            state = ConsumerState.RUNNING;
            logger.info("Consumer [" + id + "] started");
        } catch (Exception e) {
            running = false;
            state = ConsumerState.STOPPED;
            logger.log(Level.SEVERE, "Consumer [" + id + "] start failed", e);
            throw new RuntimeException("Failed to start consumer: " + id, e);
        }
    }

    public synchronized void stop() {
        if (!running) {
            logger.warning("Consumer [" + id + "] not running");
            return;
        }

        state = ConsumerState.STOPPING;
        running = false;

        try {
            consumer.wakeup();
            logger.info("Consumer [" + id + "] stopping...");
            awaitShutdown();
            consumer.close(CLOSE_TIMEOUT);
            state = ConsumerState.STOPPED;
            logger.info(String.format("Consumer [%s] stopped, consumed=%d", id, messagesConsumed.get()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Consumer [" + id + "] stop interrupted", e);
            state = ConsumerState.STOPPED;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Consumer [" + id + "] stop failed", e);
            state = ConsumerState.STOPPED;
        }
    }

    public synchronized void pause() {
        if (state != ConsumerState.RUNNING) {
            logger.warning("Consumer [" + id + "] not running, cannot pause");
            return;
        }

        try {
            consumer.pause(consumer.assignment());
            state = ConsumerState.PAUSED;
            logger.info("Consumer [" + id + "] paused");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Consumer [" + id + "] pause failed", e);
        }
    }

    public synchronized void resume() {
        if (state != ConsumerState.PAUSED) {
            logger.warning("Consumer [" + id + "] not paused, cannot resume");
            return;
        }

        try {
            consumer.resume(consumer.assignment());
            state = ConsumerState.RUNNING;
            logger.info("Consumer [" + id + "] resumed");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Consumer [" + id + "] resume failed", e);
        }
    }

    private void startPollThread() {
        pollThread = new Thread(this::pollLoop, "kafka-consumer-" + id);
        pollThread.setDaemon(false);
        pollThread.start();
    }

    private void awaitShutdown() throws InterruptedException {
        if (!shutdownLatch.await(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            logger.warning("Consumer [" + id + "] shutdown wait timeout");
            if (pollThread != null) {
                pollThread.interrupt();
            }
        }
    }

    private void pollLoop() {
        String threadName = Thread.currentThread().getName();
        logThreadStart(threadName);
        try {
            while (running) {
                if (!pollOnce(threadName)) {
                    break;
                }
            }
        } finally {
            shutdownLatch.countDown();
            logThreadStopped(threadName);
        }
    }

    private boolean pollOnce(String threadName) {
        try {
            ConsumerRecords<K, V> records = consumer.poll(POLL_TIMEOUT);
            lastPollTime = System.currentTimeMillis();
            if (!records.isEmpty()) {
                processRecords(records);
            }
            return true;
        } catch (WakeupException e) {
            return handleWakeup(threadName);
        } catch (Exception e) {
            return handlePollException(threadName, e);
        }
    }

    private boolean handleWakeup(String threadName) {
        if (running) {
            logger.warning("Consumer thread [" + threadName + "] woke up while still running");
        }
        return false;
    }

    private boolean handlePollException(String threadName, Exception e) {
        logger.log(Level.SEVERE, "Consumer thread [" + threadName + "] poll failed", e);
        if (errorHandlingStrategy == ConsumerErrorHandlingStrategy.STOP) {
            running = false;
            state = ConsumerState.STOPPED;
            return false;
        }
        return sleepBackoff();
    }

    private boolean sleepBackoff() {
        try {
            Thread.sleep(1000);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void logThreadStart(String threadName) {
        logger.info("Consumer thread [" + threadName + "] started");
    }

    private void logThreadStopped(String threadName) {
        logger.info("Consumer thread [" + threadName + "] stopped");
    }

    private void processRecords(ConsumerRecords<K, V> records) {
        for (ConsumerRecord<K, V> record : records) {
            processRecord(record);
        }
    }

    private void processRecord(ConsumerRecord<K, V> record) {
        try {
            if (enableConsumeLogging) {
                logger.info(String.format(
                        "Consume message [consumerId=%s, topic=%s, partition=%d, offset=%d, key=%s]",
                        id, record.topic(), record.partition(), record.offset(), record.key()
                ));
            }
            handler.handle(record);
            messagesConsumed.incrementAndGet();
        } catch (Exception e) {
            handleConsumerException(record, e);
        }
    }

    private void handleConsumerException(ConsumerRecord<K, V> record, Exception e) {
        logger.log(Level.SEVERE, String.format(
                "Consume failed [consumerId=%s, topic=%s, partition=%d, offset=%d, key=%s]",
                id, record.topic(), record.partition(), record.offset(), record.key()
        ), e);

        switch (errorHandlingStrategy) {
            case SKIP:
                break;
            case RETRY:
                retryRecord(record);
                break;
            case DLQ:
                logger.severe(String.format(
                        "DLQ record [topic=%s, partition=%d, offset=%d, key=%s, value=%s]",
                        record.topic(), record.partition(), record.offset(), record.key(), record.value()
                ));
                break;
            case STOP:
                logger.severe(String.format("Consumer [%s] stopped due to errors", id));
                running = false;
                state = ConsumerState.STOPPED;
                break;
        }
    }

    private void retryRecord(ConsumerRecord<K, V> record) {
        try {
            logger.info(String.format("Retry consume [topic=%s, partition=%d, offset=%d]",
                    record.topic(), record.partition(), record.offset()));
            handler.handle(record);
            messagesConsumed.incrementAndGet();
        } catch (Exception retryEx) {
            logger.log(Level.SEVERE, String.format(
                    "Retry consume failed [topic=%s, partition=%d, offset=%d]",
                    record.topic(), record.partition(), record.offset()
            ), retryEx);
        }
    }
}
