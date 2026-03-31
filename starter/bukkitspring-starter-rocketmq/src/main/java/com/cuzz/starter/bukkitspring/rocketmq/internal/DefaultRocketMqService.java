package com.cuzz.starter.bukkitspring.rocketmq.internal;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.bukkitspring.api.annotation.PreDestroy;
import com.cuzz.starter.bukkitspring.rocketmq.api.RocketMqService;
import com.cuzz.starter.bukkitspring.rocketmq.config.RocketMqSettings;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Component
public final class DefaultRocketMqService implements RocketMqService {
    private final RocketMqSettings settings;
    private final Logger logger;
    private final RocketMqClientFactory clientFactory;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object executorLock = new Object();
    private final Object defaultProducerLock = new Object();
    private final Set<DefaultMQProducer> producers = ConcurrentHashMap.newKeySet();
    private final Set<DefaultMQPushConsumer> consumers = ConcurrentHashMap.newKeySet();
    private volatile ExecutorService executor;
    private volatile DefaultMQProducer defaultProducer;

    @Autowired
    public DefaultRocketMqService(RocketMqSettings settings, Logger logger) {
        this.settings = settings;
        this.logger = logger;
        this.clientFactory = new RocketMqClientFactory(settings, getClass().getClassLoader());
    }

    @PostConstruct
    public void registerGlobalBean() {
        if (!settings.enabled) {
            logInfo("[RocketMQ] Disabled, skip global bean registration.");
            return;
        }
        registerGlobalBeanInternal();
    }

    @PreDestroy
    public void preDestroy() {
        close();
    }

    @Override
    public boolean isEnabled() {
        return settings.enabled && !closed.get();
    }

    @Override
    public ExecutorService executor() {
        if (closed.get()) {
            throw new IllegalStateException("RocketMQ service is closed.");
        }
        ExecutorService current = executor;
        if (current != null) {
            return current;
        }
        synchronized (executorLock) {
            if (executor == null) {
                executor = RocketMqExecutors.create(settings.useVirtualThreads, logger);
            }
            return executor;
        }
    }

    @Override
    public RocketMqSettings settings() {
        return settings;
    }

    @Override
    public DefaultMQProducer defaultProducer() {
        ensureEnabled();
        DefaultMQProducer current = defaultProducer;
        if (current != null) {
            return current;
        }
        synchronized (defaultProducerLock) {
            if (defaultProducer == null) {
                defaultProducer = createProducer(Map.of());
            }
            return defaultProducer;
        }
    }

    @Override
    public DefaultMQProducer createProducer(Map<String, Object> overrides) {
        ensureEnabled();
        DefaultMQProducer producer = clientFactory.createProducer(overrides);
        producers.add(producer);
        return producer;
    }

    @Override
    public DefaultMQPushConsumer createPushConsumer(String consumerGroup, Map<String, Object> overrides) {
        ensureEnabled();
        DefaultMQPushConsumer consumer = clientFactory.createPushConsumer(consumerGroup, overrides);
        consumers.add(consumer);
        return consumer;
    }

    @Override
    public DefaultMQPushConsumer subscribeConcurrently(String consumerGroup,
                                                       String topic,
                                                       String subExpression,
                                                       MessageListenerConcurrently listener,
                                                       Map<String, Object> overrides) {
        ensureEnabled();
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        String normalizedTopic = normalize(topic);
        if (normalizedTopic.isEmpty()) {
            throw new IllegalArgumentException("topic cannot be blank");
        }
        String expression = normalizeOrDefault(subExpression, "*");

        DefaultMQPushConsumer consumer = createPushConsumer(consumerGroup, overrides);
        try {
            consumer.subscribe(normalizedTopic, expression);
            consumer.registerMessageListener(listener);
            consumer.start();
            return consumer;
        } catch (Exception e) {
            consumers.remove(consumer);
            safeShutdownConsumer(consumer);
            throw new IllegalStateException("Failed to start RocketMQ consumer for topic: " + normalizedTopic, e);
        }
    }

    @Override
    public SendResult send(Message message) {
        ensureEnabled();
        try {
            return defaultProducer().send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send RocketMQ message.", e);
        }
    }

    @Override
    public SendResult send(String topic, String body) {
        return send(createMessage(topic, "", body));
    }

    @Override
    public SendResult send(String topic, String tags, String body) {
        return send(createMessage(topic, tags, body));
    }

    @Override
    public CompletableFuture<SendResult> sendAsync(Message message) {
        ensureEnabled();
        CompletableFuture<SendResult> future = new CompletableFuture<>();
        try {
            defaultProducer().send(message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    future.complete(sendResult);
                }

                @Override
                public void onException(Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<SendResult> sendAsync(String topic, String body) {
        return sendAsync(createMessage(topic, "", body));
    }

    @Override
    public CompletableFuture<SendResult> sendAsync(String topic, String tags, String body) {
        return sendAsync(createMessage(topic, tags, body));
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        for (DefaultMQPushConsumer consumer : consumers) {
            safeShutdownConsumer(consumer);
        }
        consumers.clear();

        for (DefaultMQProducer producer : producers) {
            safeShutdownProducer(producer);
        }
        producers.clear();
        defaultProducer = null;

        ExecutorService currentExecutor = executor;
        if (currentExecutor != null) {
            currentExecutor.shutdown();
        }
    }

    private Message createMessage(String topic, String tags, String body) {
        String normalizedTopic = normalize(topic);
        if (normalizedTopic.isEmpty()) {
            throw new IllegalArgumentException("topic cannot be blank");
        }
        byte[] payload = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        String normalizedTags = normalize(tags);
        if (normalizedTags.isEmpty()) {
            return new Message(normalizedTopic, payload);
        }
        return new Message(normalizedTopic, normalizedTags, payload);
    }

    private void ensureEnabled() {
        if (!settings.enabled) {
            throw new IllegalStateException("RocketMQ is disabled (rocketmq.enabled=false).");
        }
        if (closed.get()) {
            throw new IllegalStateException("RocketMQ service is closed.");
        }
    }

    private void safeShutdownConsumer(DefaultMQPushConsumer consumer) {
        if (consumer == null) {
            return;
        }
        try {
            consumer.shutdown();
        } catch (Exception e) {
            logWarning("[RocketMQ] Failed to shutdown consumer: " + e.getMessage());
        }
    }

    private void safeShutdownProducer(DefaultMQProducer producer) {
        if (producer == null) {
            return;
        }
        try {
            producer.shutdown();
        } catch (Exception e) {
            logWarning("[RocketMQ] Failed to shutdown producer: " + e.getMessage());
        }
    }

    private void registerGlobalBeanInternal() {
        try {
            Class<?> bukkitSpringClass = Class.forName("com.cuzz.bukkitspring.BukkitSpring");
            java.lang.reflect.Method registerMethod = bukkitSpringClass
                    .getMethod("registerGlobalBean", Class.class, Object.class);
            registerMethod.invoke(null, RocketMqService.class, this);
            logInfo("[RocketMQ] RocketMqService registered as global bean");
        } catch (Exception e) {
            logWarning("[RocketMQ] Failed to register global bean: " + e.getMessage());
        }
    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    private void logWarning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String normalizeOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized;
    }
}
