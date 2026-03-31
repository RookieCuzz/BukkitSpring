package com.cuzz.starter.bukkitspring.rocketmq.config;

import com.cuzz.starter.bukkitspring.rocketmq.testutil.MapConfigView;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RocketMqSettingsTest {

    @Test
    public void defaultsWhenConfigMissing() {
        RocketMqSettings settings = RocketMqSettings.fromConfig(null);

        assertFalse(settings.enabled);
        assertTrue(settings.useVirtualThreads);
        assertEquals("127.0.0.1:9876", settings.namesrvAddr);
        assertEquals("LOCAL", settings.accessChannel);
        assertEquals("bukkitspring-producer", settings.producerGroup);
        assertEquals(3000, settings.sendTimeoutMillis);
        assertEquals("bukkitspring-consumer", settings.consumerGroup);
        assertEquals("CONSUME_FROM_LAST_OFFSET", settings.consumeFromWhere);
        assertEquals("CLUSTERING", settings.messageModel);
    }

    @Test
    public void parsesAndClampsValues() {
        Map<String, Object> values = new HashMap<>();
        values.put("rocketmq.enabled", true);
        values.put("rocketmq.virtual-threads", false);
        values.put("rocketmq.name-server", "10.0.0.1:9876");
        values.put("rocketmq.namesrv-addr", "  ");
        values.put("rocketmq.access-channel", " cloud ");
        values.put("rocketmq.producer.send-timeout-ms", -10);
        values.put("rocketmq.producer.retry-times-when-send-failed", -1);
        values.put("rocketmq.producer.retry-times-when-send-async-failed", 99);
        values.put("rocketmq.consumer.consume-thread-min", 0);
        values.put("rocketmq.consumer.consume-thread-max", 999);
        values.put("rocketmq.consumer.max-reconsume-times", 999);
        values.put("rocketmq.consumer.consume-from-where", "consume_from_timestamp");
        values.put("rocketmq.consumer.message-model", "broadcasting");

        RocketMqSettings settings = RocketMqSettings.fromConfig(new MapConfigView(values));

        assertTrue(settings.enabled);
        assertFalse(settings.useVirtualThreads);
        assertEquals("10.0.0.1:9876", settings.namesrvAddr);
        assertEquals("CLOUD", settings.accessChannel);
        assertEquals(500, settings.sendTimeoutMillis);
        assertEquals(0, settings.retryTimesWhenSendFailed);
        assertEquals(16, settings.retryTimesWhenSendAsyncFailed);
        assertEquals(1, settings.consumeThreadMin);
        assertEquals(512, settings.consumeThreadMax);
        assertEquals(128, settings.maxReconsumeTimes);
        assertEquals("CONSUME_FROM_TIMESTAMP", settings.consumeFromWhere);
        assertEquals("BROADCASTING", settings.messageModel);
    }

    @Test
    public void consumeThreadMaxWillNotBeLowerThanMin() {
        Map<String, Object> values = Map.of(
                "rocketmq.consumer.consume-thread-min", 60,
                "rocketmq.consumer.consume-thread-max", 8
        );

        RocketMqSettings settings = RocketMqSettings.fromConfig(new MapConfigView(values));

        assertEquals(60, settings.consumeThreadMin);
        assertEquals(60, settings.consumeThreadMax);
    }
}
