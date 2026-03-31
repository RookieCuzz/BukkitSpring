package com.cuzz.starter.bukkitspring.rocketmq.config;

import com.cuzz.bukkitspring.spi.config.ConfigSection;
import com.cuzz.bukkitspring.spi.config.ConfigView;

import java.util.Locale;

/**
 * RocketMQ configuration settings.
 */
public final class RocketMqSettings {
    public final boolean enabled;
    public final boolean useVirtualThreads;
    public final String namesrvAddr;
    public final String namespace;
    public final String instanceName;
    public final String accessChannel;
    public final boolean useTls;
    public final boolean vipChannelEnabled;
    public final String producerGroup;
    public final int sendTimeoutMillis;
    public final int retryTimesWhenSendFailed;
    public final int retryTimesWhenSendAsyncFailed;
    public final String consumerGroup;
    public final String consumeFromWhere;
    public final String messageModel;
    public final int consumeThreadMin;
    public final int consumeThreadMax;
    public final int maxReconsumeTimes;
    public final String consumeTimestamp;

    private RocketMqSettings(boolean enabled,
                             boolean useVirtualThreads,
                             String namesrvAddr,
                             String namespace,
                             String instanceName,
                             String accessChannel,
                             boolean useTls,
                             boolean vipChannelEnabled,
                             String producerGroup,
                             int sendTimeoutMillis,
                             int retryTimesWhenSendFailed,
                             int retryTimesWhenSendAsyncFailed,
                             String consumerGroup,
                             String consumeFromWhere,
                             String messageModel,
                             int consumeThreadMin,
                             int consumeThreadMax,
                             int maxReconsumeTimes,
                             String consumeTimestamp) {
        this.enabled = enabled;
        this.useVirtualThreads = useVirtualThreads;
        this.namesrvAddr = namesrvAddr;
        this.namespace = namespace;
        this.instanceName = instanceName;
        this.accessChannel = accessChannel;
        this.useTls = useTls;
        this.vipChannelEnabled = vipChannelEnabled;
        this.producerGroup = producerGroup;
        this.sendTimeoutMillis = sendTimeoutMillis;
        this.retryTimesWhenSendFailed = retryTimesWhenSendFailed;
        this.retryTimesWhenSendAsyncFailed = retryTimesWhenSendAsyncFailed;
        this.consumerGroup = consumerGroup;
        this.consumeFromWhere = consumeFromWhere;
        this.messageModel = messageModel;
        this.consumeThreadMin = consumeThreadMin;
        this.consumeThreadMax = consumeThreadMax;
        this.maxReconsumeTimes = maxReconsumeTimes;
        this.consumeTimestamp = consumeTimestamp;
    }

    public static RocketMqSettings fromConfig(ConfigView config) {
        ConfigView safeConfig = config == null ? EmptyConfigView.INSTANCE : config;

        boolean enabled = safeConfig.getBoolean("rocketmq.enabled", false);
        boolean useVirtualThreads = safeConfig.getBoolean("rocketmq.virtual-threads", true);
        String namesrvAddrPrimary = normalize(safeConfig.getString("rocketmq.namesrv-addr", ""));
        String namesrvAddrFallback = normalizeOrDefault(
                safeConfig.getString("rocketmq.name-server", "127.0.0.1:9876"),
                "127.0.0.1:9876"
        );
        String namesrvAddr = namesrvAddrPrimary.isEmpty() ? namesrvAddrFallback : namesrvAddrPrimary;
        String namespace = normalize(safeConfig.getString("rocketmq.namespace", ""));
        String instanceName = normalizeOrDefault(safeConfig.getString("rocketmq.instance-name", "bukkitspring"), "bukkitspring");
        String accessChannel = normalizeUpper(safeConfig.getString("rocketmq.access-channel", "LOCAL"));
        boolean useTls = safeConfig.getBoolean("rocketmq.use-tls", false);
        boolean vipChannelEnabled = safeConfig.getBoolean("rocketmq.vip-channel-enabled", false);

        String producerGroup = normalizeOrDefault(
                safeConfig.getString("rocketmq.producer.group", "bukkitspring-producer"),
                "bukkitspring-producer"
        );
        int sendTimeoutMillis = clampInt(safeConfig.getInt("rocketmq.producer.send-timeout-ms", 3000), 500, 120000);
        int retryTimesWhenSendFailed = clampInt(
                safeConfig.getInt("rocketmq.producer.retry-times-when-send-failed", 2),
                0,
                16
        );
        int retryTimesWhenSendAsyncFailed = clampInt(
                safeConfig.getInt("rocketmq.producer.retry-times-when-send-async-failed", 2),
                0,
                16
        );

        String consumerGroup = normalizeOrDefault(
                safeConfig.getString("rocketmq.consumer.group", "bukkitspring-consumer"),
                "bukkitspring-consumer"
        );
        String consumeFromWhere = normalizeUpper(
                safeConfig.getString("rocketmq.consumer.consume-from-where", "CONSUME_FROM_LAST_OFFSET")
        );
        String messageModel = normalizeUpper(safeConfig.getString("rocketmq.consumer.message-model", "CLUSTERING"));
        int consumeThreadMin = clampInt(safeConfig.getInt("rocketmq.consumer.consume-thread-min", 20), 1, 256);
        int consumeThreadMax = clampInt(safeConfig.getInt("rocketmq.consumer.consume-thread-max", 64), consumeThreadMin, 512);
        int maxReconsumeTimes = clampInt(safeConfig.getInt("rocketmq.consumer.max-reconsume-times", 16), -1, 128);
        String consumeTimestamp = normalize(safeConfig.getString("rocketmq.consumer.consume-timestamp", ""));

        return new RocketMqSettings(
                enabled,
                useVirtualThreads,
                namesrvAddr,
                namespace,
                instanceName,
                accessChannel,
                useTls,
                vipChannelEnabled,
                producerGroup,
                sendTimeoutMillis,
                retryTimesWhenSendFailed,
                retryTimesWhenSendAsyncFailed,
                consumerGroup,
                consumeFromWhere,
                messageModel,
                consumeThreadMin,
                consumeThreadMax,
                maxReconsumeTimes,
                consumeTimestamp
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String normalizeUpper(String value) {
        return normalize(value).toUpperCase(Locale.ROOT);
    }

    private static String normalizeOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized;
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

    private enum EmptyConfigView implements ConfigView {
        INSTANCE;

        @Override
        public boolean getBoolean(String path, boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public String getString(String path, String defaultValue) {
            return defaultValue;
        }

        @Override
        public int getInt(String path, int defaultValue) {
            return defaultValue;
        }

        @Override
        public long getLong(String path, long defaultValue) {
            return defaultValue;
        }

        @Override
        public ConfigSection getSection(String path) {
            return null;
        }
    }
}
