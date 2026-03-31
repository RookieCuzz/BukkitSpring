package com.cuzz.starter.bukkitspring.kafka.internal.consumer;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.starter.bukkitspring.kafka.api.ConsumerInfo;
import com.cuzz.starter.bukkitspring.kafka.api.ConsumerRegistration;
import com.cuzz.starter.bukkitspring.kafka.api.ConsumerState;
import com.cuzz.starter.bukkitspring.kafka.api.ConsumerStatus;
import com.cuzz.starter.bukkitspring.kafka.config.KafkaConsumerManagerSettings;
import com.cuzz.starter.bukkitspring.kafka.config.KafkaSettings;
import com.cuzz.starter.bukkitspring.kafka.internal.util.ContextClassLoaderRunner;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
public final class DefaultKafkaConsumerManager {

    private static final String DEFAULT_KEY_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";
    private static final String DEFAULT_VALUE_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";

    private final KafkaSettings settings;
    private final Logger logger;
    private final KafkaConsumerManagerSettings settingsManager;
    private final ClassLoader classLoader;
    private final Map<String, ManagedConsumer<?, ?>> consumers = new ConcurrentHashMap<>();
    private final Map<String, ConsumerRegistration<?, ?>> registrations = new ConcurrentHashMap<>();

    @Autowired
    public DefaultKafkaConsumerManager(KafkaSettings settings,
                                       Logger logger,
                                       KafkaConsumerManagerSettings settingsManager) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.settingsManager = Objects.requireNonNull(settingsManager, "settingsManager");
        this.classLoader = DefaultKafkaConsumerManager.class.getClassLoader();
    }

    public <K, V> String registerConsumer(ConsumerRegistration<K, V> registration) {
        Objects.requireNonNull(registration, "registration");
        ensureEnabled();
        String consumerId = generateConsumerId(registration);
        assertConsumerIdAvailable(consumerId);
        registrations.put(consumerId, registration);
        logRegistration(consumerId, registration);
        return consumerId;
    }

    public void startAll() {
        logger.info("Starting auto-start consumers...");
        int started = 0;
        for (Map.Entry<String, ConsumerRegistration<?, ?>> entry : registrations.entrySet()) {
            String consumerId = entry.getKey();
            ConsumerRegistration<?, ?> registration = entry.getValue();
            if (registration.isAutoStartup() && !consumers.containsKey(consumerId)) {
                try {
                    startConsumer(consumerId, registration);
                    started++;
                } catch (Exception e) {
                    logger.severe(String.format("Failed to start consumer [%s]: %s", consumerId, e.getMessage()));
                }
            }
        }
        logger.info(String.format("Auto-start consumers started=%d", started));
    }

    public void stopAll() {
        if (consumers.isEmpty()) {
            logger.info("No running consumers to stop.");
            return;
        }
        logger.info("Stopping consumers count=" + consumers.size());
        List<Exception> errors = new ArrayList<>();
        for (Map.Entry<String, ManagedConsumer<?, ?>> entry : consumers.entrySet()) {
            String consumerId = entry.getKey();
            ManagedConsumer<?, ?> consumer = entry.getValue();
            try {
                consumer.stop();
            } catch (Exception e) {
                errors.add(e);
                logger.log(Level.SEVERE, "Failed to stop consumer [" + consumerId + "]", e);
            }
        }
        consumers.clear();
        if (errors.isEmpty()) {
            logger.info("All consumers stopped.");
        } else {
            logger.info("All consumers stopped with errors count=" + errors.size());
        }
    }

    public void start(String consumerId) {
        Objects.requireNonNull(consumerId, "consumerId");
        ManagedConsumer<?, ?> existing = consumers.get(consumerId);
        if (existing != null) {
            logger.warning(String.format("Consumer [%s] already started", consumerId));
            return;
        }
        ConsumerRegistration<?, ?> registration = registrations.get(consumerId);
        if (registration == null) {
            throw new IllegalArgumentException("Consumer not registered: " + consumerId);
        }
        startConsumer(consumerId, registration);
    }

    public void stop(String consumerId) {
        Objects.requireNonNull(consumerId, "consumerId");
        ManagedConsumer<?, ?> consumer = consumers.remove(consumerId);
        if (consumer == null) {
            logger.warning(String.format("Consumer [%s] not running", consumerId));
            return;
        }
        consumer.stop();
    }

    public void pause(String consumerId) {
        Objects.requireNonNull(consumerId, "consumerId");
        ManagedConsumer<?, ?> consumer = consumers.get(consumerId);
        if (consumer == null) {
            throw new IllegalArgumentException("Consumer not running: " + consumerId);
        }
        consumer.pause();
    }

    public void resume(String consumerId) {
        Objects.requireNonNull(consumerId, "consumerId");
        ManagedConsumer<?, ?> consumer = consumers.get(consumerId);
        if (consumer == null) {
            throw new IllegalArgumentException("Consumer not running: " + consumerId);
        }
        consumer.resume();
    }

    public ConsumerStatus getStatus(String consumerId) {
        Objects.requireNonNull(consumerId, "consumerId");
        ManagedConsumer<?, ?> consumer = consumers.get(consumerId);
        if (consumer == null) {
            return null;
        }
        return new ConsumerStatus(
                consumer.getId(),
                consumer.getTopics(),
                consumer.getGroupId(),
                consumer.getState(),
                consumer.getMessagesConsumed(),
                consumer.getLastPollTime(),
                Collections.emptyMap(),
                consumer.getConcurrency()
        );
    }

    public List<ConsumerInfo> listAll() {
        return registrations.entrySet().stream()
                .map(entry -> {
                    String id = entry.getKey();
                    ConsumerRegistration<?, ?> reg = entry.getValue();
                    ManagedConsumer<?, ?> consumer = consumers.get(id);
                    ConsumerState state = consumer != null ? consumer.getState() : ConsumerState.STOPPED;
                    return new ConsumerInfo(
                            id,
                            reg.getTopics(),
                            reg.getGroupId(),
                            state,
                            reg.getConcurrency(),
                            reg.isAutoStartup()
                    );
                })
                .collect(Collectors.toList());
    }

    private <K, V> void startConsumer(String consumerId, ConsumerRegistration<K, V> registration) {
        try {
            Map<String, Object> consumerProps = buildConsumerProperties(registration);
            validateBootstrapServers(consumerProps);
            KafkaConsumer<K, V> kafkaConsumer = createKafkaConsumer(consumerProps);
            ManagedConsumer<K, V> managedConsumer = createManagedConsumer(consumerId, registration, kafkaConsumer);
            startManagedConsumer(consumerId, managedConsumer);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start consumer [" + consumerId + "]", e);
            throw new RuntimeException("Failed to start consumer: " + consumerId, e);
        }
    }

    private void validateBootstrapServers(Map<String, Object> consumerProps) {
        Object value = consumerProps.get("bootstrap.servers");
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalStateException("Kafka bootstrap.servers is required.");
        }
    }

    private <K, V> KafkaConsumer<K, V> createKafkaConsumer(Map<String, Object> consumerProps) {
        return ContextClassLoaderRunner.runWith(classLoader, () -> new KafkaConsumer<>(consumerProps));
    }

    private <K, V> ManagedConsumer<K, V> createManagedConsumer(String consumerId,
                                                               ConsumerRegistration<K, V> registration,
                                                               KafkaConsumer<K, V> kafkaConsumer) {
        return new ManagedConsumer<>(
                consumerId,
                kafkaConsumer,
                registration.getTopics(),
                registration.getGroupId(),
                registration.getHandler(),
                logger,
                settingsManager.errorHandlingStrategy,
                settingsManager.enableConsumeLogging
        );
    }

    private void startManagedConsumer(String consumerId, ManagedConsumer<?, ?> managedConsumer) {
        managedConsumer.start();
        consumers.put(consumerId, managedConsumer);
    }

    private <K, V> Map<String, Object> buildConsumerProperties(ConsumerRegistration<K, V> registration) {
        Map<String, Object> props = new LinkedHashMap<>();
        mergeProperties(props, settings.baseProperties);
        mergeProperties(props, settings.consumerProperties);
        mergeProperties(props, registration.getProperties());
        applyGroupId(props, registration);
        props.putIfAbsent("key.deserializer", DEFAULT_KEY_DESERIALIZER);
        props.putIfAbsent("value.deserializer", DEFAULT_VALUE_DESERIALIZER);
        applyDefaultBaseProperties(props);
        return props;
    }

    private void applyDefaultBaseProperties(Map<String, Object> props) {
        applyIfBlank(props, "bootstrap.servers", settings.bootstrapServers);
        applyIfBlank(props, "client.id", settings.clientId);
    }

    private static void mergeProperties(Map<String, Object> target, Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        target.putAll(source);
    }

    private static <K, V> void applyGroupId(Map<String, Object> props, ConsumerRegistration<K, V> registration) {
        String groupId = registration.getGroupId();
        if (groupId != null && !groupId.isBlank()) {
            props.put("group.id", groupId);
        }
    }

    private static void applyIfBlank(Map<String, Object> props, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Object existing = props.get(key);
        if (existing == null || String.valueOf(existing).isBlank()) {
            props.put(key, value);
        }
    }

    private void ensureEnabled() {
        if (!settings.enabled) {
            throw new IllegalStateException("Kafka is disabled.");
        }
    }

    private void assertConsumerIdAvailable(String consumerId) {
        if (registrations.containsKey(consumerId)) {
            throw new IllegalArgumentException("Consumer ID already exists: " + consumerId);
        }
    }

    private void logRegistration(String consumerId, ConsumerRegistration<?, ?> registration) {
        if (!logger.isLoggable(Level.INFO)) {
            return;
        }
        logger.info(String.format("Registered consumer [id=%s, topics=%s, groupId=%s, autoStartup=%s]",
                consumerId, registration.getTopics(), registration.getGroupId(), registration.isAutoStartup()));
    }

    private <K, V> String generateConsumerId(ConsumerRegistration<K, V> registration) {
        if (registration.getId() != null && !registration.getId().isBlank()) {
            return registration.getId().trim();
        }
        String topicsStr = String.join("-", registration.getTopics());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s-%s-%s", registration.getGroupId(), topicsStr, uuid);
    }
}
