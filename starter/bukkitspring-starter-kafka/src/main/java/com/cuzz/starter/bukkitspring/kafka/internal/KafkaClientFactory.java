package com.cuzz.starter.bukkitspring.kafka.internal;

import com.cuzz.starter.bukkitspring.kafka.config.KafkaSettings;
import com.cuzz.starter.bukkitspring.kafka.internal.util.ContextClassLoaderRunner;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class KafkaClientFactory {
    private final ClassLoader classLoader;
    private final Map<String, Object> baseProperties;
    private final Map<String, Object> producerProperties;
    private final Map<String, Object> consumerProperties;
    private final Map<String, Object> adminProperties;

    KafkaClientFactory(KafkaSettings settings, ClassLoader classLoader) {
        Objects.requireNonNull(settings, "settings");
        this.classLoader = classLoader;
        this.baseProperties = buildBaseProperties(settings);
        this.producerProperties = settings.producerProperties;
        this.consumerProperties = settings.consumerProperties;
        this.adminProperties = settings.adminProperties;
    }

    Map<String, Object> baseProperties() {
        return baseProperties;
    }

    <K, V> KafkaProducer<K, V> createProducer(Map<String, Object> overrides) {
        Map<String, Object> props = buildClientProperties(producerProperties, overrides);
        return ContextClassLoaderRunner.runWith(classLoader, () -> new KafkaProducer<>(props));
    }

    <K, V> KafkaConsumer<K, V> createConsumer(String groupId, Map<String, Object> overrides) {
        Map<String, Object> props = buildClientProperties(consumerProperties, overrides);
        if (groupId != null && !groupId.isBlank()) {
            props.put("group.id", groupId);
        }
        Object resolvedGroup = props.get("group.id");
        if (resolvedGroup == null || String.valueOf(resolvedGroup).isBlank()) {
            throw new IllegalArgumentException("Kafka group.id is required for consumers.");
        }
        return ContextClassLoaderRunner.runWith(classLoader, () -> new KafkaConsumer<>(props));
    }

    Admin createAdmin(Map<String, Object> overrides) {
        Map<String, Object> props = buildClientProperties(adminProperties, overrides);
        return ContextClassLoaderRunner.runWith(classLoader, () -> Admin.create(props));
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
