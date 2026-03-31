package com.cuzz.starter.bukkitspring.kafka.internal;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.starter.bukkitspring.kafka.api.*;
import com.cuzz.starter.bukkitspring.kafka.config.KafkaSettings;
import com.cuzz.starter.bukkitspring.kafka.internal.consumer.DefaultKafkaConsumerManager;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Component
public final class DefaultKafkaService implements KafkaService, AutoCloseable {
    private final KafkaSettings settings;
    private final Logger logger;
    private final KafkaClientFactory clientFactory;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object executorLock = new Object();
    private volatile ExecutorService executor;
    
    @Autowired
    private DefaultKafkaConsumerManager consumerManager;

    /**
     * 构造函数 - 通过 Spring 依赖注入
     * 
     * @param settings Kafka 配置（自动注入）
     * @param logger 日志记录器（自动注入）
     */
    @Autowired
    public DefaultKafkaService(KafkaSettings settings, Logger logger) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = logger;
        this.clientFactory = new KafkaClientFactory(settings, getClass().getClassLoader());
    }
        
    /**
     * 初始化后处理 - 注册为全局 Bean
     */
    @PostConstruct
    public void registerGlobalBean() {
        if (!settings.enabled) {
            if (logger != null) {
                logger.info("[Kafka] Disabled, skip global bean registration.");
            }
            return;
        }
        try {
            Class<?> bukkitSpringClass = Class.forName("com.cuzz.bukkitspring.BukkitSpring");
            java.lang.reflect.Method registerMethod = bukkitSpringClass.getMethod("registerGlobalBean", Class.class, Object.class);
            registerMethod.invoke(null, KafkaService.class, this);
            logger.info("[Kafka] KafkaService registered as global bean");
        } catch (Exception e) {
            logger.warning("[Kafka] Failed to register global bean: " + e.getMessage());
        }
    }
        
    @Override
    public boolean isEnabled() {
        return settings.enabled;
    }

    @Override
    public ExecutorService executor() {
        if (closed.get()) {
            throw new IllegalStateException("Kafka service is closed.");
        }
        ExecutorService current = executor;
        if (current != null) {
            return current;
        }
        synchronized (executorLock) {
            if (executor == null) {
                executor = KafkaExecutors.create(settings.useVirtualThreads, logger);
            }
            return executor;
        }
    }

    @Override
    public Map<String, Object> baseProperties() {
        return clientFactory.baseProperties();
    }

    @Override
    public <K, V> KafkaProducer<K, V> createProducer(Map<String, Object> overrides) {
        ensureEnabled();
        return clientFactory.createProducer(overrides);
    }

    @Override
    public <K, V> KafkaConsumer<K, V> createConsumer(String groupId, Map<String, Object> overrides) {
        ensureEnabled();
        return clientFactory.createConsumer(groupId, overrides);
    }

    @Override
    public Admin createAdmin(Map<String, Object> overrides) {
        ensureEnabled();
        return clientFactory.createAdmin(overrides);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        // 停止所有消费者
        if (consumerManager != null) {
            consumerManager.stopAll();
        }
        ExecutorService current = executor;
        if (current != null) {
            current.shutdown();
        }
    }

    private void ensureEnabled() {
        if (!settings.enabled) {
            throw new IllegalStateException("Kafka is disabled (kafka.enabled=false).");
        }
    }

    
    // ==================== 消费者管理功能委托 ====================
    
    @Override
    public <K, V> String registerConsumer(ConsumerRegistration<K, V> registration) {
        return consumerManager.registerConsumer(registration);
    }
    
    @Override
    public void startConsumer(String consumerId) {
        consumerManager.start(consumerId);
    }
    
    @Override
    public void stopConsumer(String consumerId) {
        consumerManager.stop(consumerId);
    }
    
    @Override
    public void pauseConsumer(String consumerId) {
        consumerManager.pause(consumerId);
    }
    
    @Override
    public void resumeConsumer(String consumerId) {
        consumerManager.resume(consumerId);
    }
    
    @Override
    public ConsumerStatus getConsumerStatus(String consumerId) {
        return consumerManager.getStatus(consumerId);
    }
    
    @Override
    public List<ConsumerInfo> listConsumers() {
        return consumerManager.listAll();
    }
    
    /**
     * 启动所有自动启动的消费者
     */
    public void startAllConsumers() {
        consumerManager.startAll();
    }
}
