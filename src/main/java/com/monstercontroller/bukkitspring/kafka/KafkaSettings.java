package com.monstercontroller.bukkitspring.kafka;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.monstercontroller.bukkitspring.api.kafka.ConsumerErrorHandlingStrategy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class KafkaSettings {
    public final boolean enabled;
    public final boolean useVirtualThreads;
    public final String bootstrapServers;
    public final String clientId;
    public final Map<String, Object> baseProperties;
    public final Map<String, Object> producerProperties;
    public final Map<String, Object> consumerProperties;
    public final Map<String, Object> adminProperties;
    public final KafkaConsumerManagerSettings consumerManagerSettings;

    private KafkaSettings(boolean enabled,
                          boolean useVirtualThreads,
                          String bootstrapServers,
                          String clientId,
                          Map<String, Object> baseProperties,
                          Map<String, Object> producerProperties,
                          Map<String, Object> consumerProperties,
                          Map<String, Object> adminProperties,
                          KafkaConsumerManagerSettings consumerManagerSettings) {
        this.enabled = enabled;
        this.useVirtualThreads = useVirtualThreads;
        this.bootstrapServers = bootstrapServers;
        this.clientId = clientId;
        this.baseProperties = baseProperties;
        this.producerProperties = producerProperties;
        this.consumerProperties = consumerProperties;
        this.adminProperties = adminProperties;
        this.consumerManagerSettings = consumerManagerSettings;
    }

    public static KafkaSettings fromConfig(FileConfiguration config) {
        boolean enabled = config.getBoolean("kafka.enabled", false);
        boolean useVirtualThreads = config.getBoolean("kafka.virtual-threads", true);
        String bootstrapServers = config.getString("kafka.bootstrap-servers", "localhost:9092");
        String clientId = config.getString("kafka.client-id", "bukkitspring");

        Map<String, Object> baseProperties = readSection(config, "kafka.properties");
        Map<String, Object> producerProperties = readSection(config, "kafka.producer");
        Map<String, Object> consumerProperties = readSection(config, "kafka.consumer");
        Map<String, Object> adminProperties = readSection(config, "kafka.admin");
        KafkaConsumerManagerSettings managerSettings = readManagerSettings(config);

        return new KafkaSettings(
                enabled,
                useVirtualThreads,
                normalize(bootstrapServers),
                normalize(clientId),
                baseProperties,
                producerProperties,
                consumerProperties,
                adminProperties,
                managerSettings
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static Map<String, Object> readSection(FileConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value != null) {
                values.put(key, value);
            }
        }
        return Collections.unmodifiableMap(values);
    }

    private static KafkaConsumerManagerSettings readManagerSettings(FileConfiguration config) {
        int shutdownTimeout = clampInt(config.getInt("kafka.consumer-manager.shutdown-timeout", 30), 1, 300);
        String errorHandling = config.getString("kafka.consumer-manager.error-handling", "SKIP");
        boolean enableConsumeLogging = config.getBoolean("kafka.consumer-manager.enable-consume-logging", false);
        return new KafkaConsumerManagerSettings(
                shutdownTimeout,
                ConsumerErrorHandlingStrategy.fromString(errorHandling),
                enableConsumeLogging
        );
    }

    private static int clampInt(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
