package com.cuzz.starter.bukkitspring.rocketmq.internal;

import com.cuzz.starter.bukkitspring.rocketmq.config.RocketMqSettings;
import com.cuzz.starter.bukkitspring.rocketmq.internal.util.ContextClassLoaderRunner;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class RocketMqClientFactory {
    private final RocketMqSettings settings;
    private final ClassLoader classLoader;

    RocketMqClientFactory(RocketMqSettings settings, ClassLoader classLoader) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.classLoader = classLoader;
    }

    DefaultMQProducer createProducer(Map<String, Object> overrides) {
        Map<String, Object> safeOverrides = safeOverrides(overrides);
        return ContextClassLoaderRunner.runWith(classLoader, () -> {
            String producerGroup = normalizeOrDefault(
                    resolveString(safeOverrides, "producer-group", settings.producerGroup),
                    settings.producerGroup
            );
            DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
            applyCommonConfig(producer, safeOverrides);

            int sendTimeoutMs = clampInt(resolveInt(safeOverrides, "send-timeout-ms", settings.sendTimeoutMillis), 500, 120000);
            int retrySync = clampInt(
                    resolveInt(safeOverrides, "retry-times-when-send-failed", settings.retryTimesWhenSendFailed),
                    0,
                    16
            );
            int retryAsync = clampInt(
                    resolveInt(safeOverrides, "retry-times-when-send-async-failed", settings.retryTimesWhenSendAsyncFailed),
                    0,
                    16
            );
            boolean vipEnabled = resolveBoolean(safeOverrides, "send-message-with-vip-channel", settings.vipChannelEnabled);

            producer.setSendMsgTimeout(sendTimeoutMs);
            producer.setRetryTimesWhenSendFailed(retrySync);
            producer.setRetryTimesWhenSendAsyncFailed(retryAsync);
            producer.setSendMessageWithVIPChannel(vipEnabled);

            try {
                producer.start();
            } catch (MQClientException e) {
                throw new IllegalStateException("Failed to start RocketMQ producer.", e);
            }
            return producer;
        });
    }

    DefaultMQPushConsumer createPushConsumer(String consumerGroup, Map<String, Object> overrides) {
        Map<String, Object> safeOverrides = safeOverrides(overrides);
        return ContextClassLoaderRunner.runWith(classLoader, () -> {
            String group = normalizeOrDefault(
                    normalizeOrDefault(consumerGroup, resolveString(safeOverrides, "consumer-group", settings.consumerGroup)),
                    settings.consumerGroup
            );
            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(group);
            applyCommonConfig(consumer, safeOverrides);

            int consumeThreadMin = clampInt(
                    resolveInt(safeOverrides, "consume-thread-min", settings.consumeThreadMin),
                    1,
                    256
            );
            int consumeThreadMax = clampInt(
                    resolveInt(safeOverrides, "consume-thread-max", settings.consumeThreadMax),
                    consumeThreadMin,
                    512
            );
            int maxReconsumeTimes = clampInt(
                    resolveInt(safeOverrides, "max-reconsume-times", settings.maxReconsumeTimes),
                    -1,
                    128
            );
            String consumeFromWhere = resolveString(safeOverrides, "consume-from-where", settings.consumeFromWhere);
            String messageModel = resolveString(safeOverrides, "message-model", settings.messageModel);
            String consumeTimestamp = normalize(resolveString(safeOverrides, "consume-timestamp", settings.consumeTimestamp));

            consumer.setConsumeThreadMin(consumeThreadMin);
            consumer.setConsumeThreadMax(consumeThreadMax);
            consumer.setMaxReconsumeTimes(maxReconsumeTimes);
            consumer.setConsumeFromWhere(resolveConsumeFromWhere(consumeFromWhere));
            consumer.setMessageModel(resolveMessageModel(messageModel));
            if (!consumeTimestamp.isEmpty()) {
                consumer.setConsumeTimestamp(consumeTimestamp);
            }
            return consumer;
        });
    }

    private void applyCommonConfig(ClientConfig client, Map<String, Object> overrides) {
        String namesrvAddr = normalizeOrDefault(resolveString(overrides, "namesrv-addr", settings.namesrvAddr), settings.namesrvAddr);
        if (!namesrvAddr.isEmpty()) {
            client.setNamesrvAddr(namesrvAddr);
        }

        String namespace = normalize(resolveString(overrides, "namespace", settings.namespace));
        if (!namespace.isEmpty()) {
            client.setNamespace(namespace);
        }

        String instanceName = normalize(resolveString(overrides, "instance-name", settings.instanceName));
        if (!instanceName.isEmpty()) {
            client.setInstanceName(instanceName);
        }

        boolean useTls = resolveBoolean(overrides, "use-tls", settings.useTls);
        boolean vipChannelEnabled = resolveBoolean(overrides, "vip-channel-enabled", settings.vipChannelEnabled);
        String accessChannel = resolveString(overrides, "access-channel", settings.accessChannel);

        client.setUseTLS(useTls);
        client.setVipChannelEnabled(vipChannelEnabled);
        client.setAccessChannel(resolveAccessChannel(accessChannel));
    }

    private static AccessChannel resolveAccessChannel(String value) {
        String normalized = normalizeUpper(value);
        if (normalized.isEmpty()) {
            return AccessChannel.LOCAL;
        }
        try {
            return AccessChannel.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return AccessChannel.LOCAL;
        }
    }

    private static ConsumeFromWhere resolveConsumeFromWhere(String value) {
        String normalized = normalizeUpper(value);
        if (normalized.isEmpty()) {
            return ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;
        }
        try {
            return ConsumeFromWhere.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET;
        }
    }

    private static MessageModel resolveMessageModel(String value) {
        String normalized = normalizeUpper(value);
        if (normalized.isEmpty()) {
            return MessageModel.CLUSTERING;
        }
        try {
            return MessageModel.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return MessageModel.CLUSTERING;
        }
    }

    private static Map<String, Object> safeOverrides(Map<String, Object> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return Collections.emptyMap();
        }
        return overrides;
    }

    private static String resolveString(Map<String, Object> values, String key, String fallback) {
        if (values == null || key == null) {
            return fallback;
        }
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        String normalized = normalize(value.toString());
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized;
    }

    private static int resolveInt(Map<String, Object> values, String key, int fallback) {
        if (values == null || key == null) {
            return fallback;
        }
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean resolveBoolean(Map<String, Object> values, String key, boolean fallback) {
        if (values == null || key == null) {
            return fallback;
        }
        Object value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String normalized = value.toString().trim();
        if (normalized.isEmpty()) {
            return fallback;
        }
        return "true".equalsIgnoreCase(normalized)
                || "1".equals(normalized)
                || "yes".equalsIgnoreCase(normalized)
                || "on".equalsIgnoreCase(normalized);
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

    private static String normalizeUpper(String value) {
        return normalize(value).toUpperCase(Locale.ROOT);
    }
}
