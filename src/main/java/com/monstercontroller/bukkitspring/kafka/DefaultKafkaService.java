package com.monstercontroller.bukkitspring.kafka;

import com.monstercontroller.bukkitspring.api.kafka.KafkaService;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class DefaultKafkaService implements KafkaService, AutoCloseable {
    private final KafkaSettings settings;
    private final Logger logger;
    private final Map<String, Object> baseProperties;
    private final Map<String, Object> producerProperties;
    private final Map<String, Object> consumerProperties;
    private final Map<String, Object> adminProperties;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object executorLock = new Object();
    private volatile ExecutorService executor;

    public DefaultKafkaService(KafkaSettings settings, Logger logger) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = logger;
        this.baseProperties = buildBaseProperties(settings);
        this.producerProperties = settings.producerProperties;
        this.consumerProperties = settings.consumerProperties;
        this.adminProperties = settings.adminProperties;
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
        return baseProperties;
    }

    @Override
    public <K, V> KafkaProducer<K, V> createProducer(Map<String, Object> overrides) {
        ensureEnabled();
        Map<String, Object> props = buildClientProperties(producerProperties, overrides);
        return new KafkaProducer<>(props);
    }

    @Override
    public <K, V> KafkaConsumer<K, V> createConsumer(String groupId, Map<String, Object> overrides) {
        ensureEnabled();
        Map<String, Object> props = buildClientProperties(consumerProperties, overrides);
        if (groupId != null && !groupId.isBlank()) {
            props.put("group.id", groupId);
        }
        Object resolvedGroup = props.get("group.id");
        if (resolvedGroup == null || String.valueOf(resolvedGroup).isBlank()) {
            throw new IllegalArgumentException("Kafka group.id is required for consumers.");
        }
        return new KafkaConsumer<>(props);
    }

    @Override
    public Admin createAdmin(Map<String, Object> overrides) {
        ensureEnabled();
        Map<String, Object> props = buildClientProperties(adminProperties, overrides);
        return Admin.create(props);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
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

    private Map<String, Object> buildClientProperties(Map<String, Object> defaults, Map<String, Object> overrides) {
        Map<String, Object> props = new LinkedHashMap<>(baseProperties);
        if (defaults != null && !defaults.isEmpty()) {
            props.putAll(defaults);
        }
        if (overrides != null && !overrides.isEmpty()) {
            props.putAll(overrides);
        }
        validateBootstrapServers(props);
        return props;
    }

    private void validateBootstrapServers(Map<String, Object> props) {
        Object value = props.get("bootstrap.servers");
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalStateException("Kafka bootstrap.servers is required.");
        }
    }

    private static Map<String, Object> buildBaseProperties(KafkaSettings settings) {
        Map<String, Object> props = new LinkedHashMap<>(settings.baseProperties);
        if (!settings.bootstrapServers.isBlank()) {
            props.putIfAbsent("bootstrap.servers", settings.bootstrapServers);
        }
        if (!settings.clientId.isBlank()) {
            props.putIfAbsent("client.id", settings.clientId);
        }
        return Collections.unmodifiableMap(props);
    }
}
