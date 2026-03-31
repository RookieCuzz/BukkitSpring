package com.cuzz.starter.bukkitspring.rocketmq.internal;

import com.cuzz.starter.bukkitspring.rocketmq.config.RocketMqSettings;
import com.cuzz.starter.bukkitspring.rocketmq.testutil.MapConfigView;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RocketMqClientFactoryTest {

    @Test
    public void createPushConsumerAppliesOverridesAndFallbacks() {
        Map<String, Object> values = new HashMap<>();
        values.put("rocketmq.enabled", true);
        values.put("rocketmq.namesrv-addr", "127.0.0.1:9876");
        values.put("rocketmq.consumer.group", "default-consumer-group");
        values.put("rocketmq.instance-name", "test-instance");

        RocketMqSettings settings = RocketMqSettings.fromConfig(new MapConfigView(values));
        RocketMqClientFactory factory = new RocketMqClientFactory(settings, getClass().getClassLoader());

        Map<String, Object> overrides = new HashMap<>();
        overrides.put("consumer-group", "override-consumer-group");
        overrides.put("consume-thread-min", 8);
        overrides.put("consume-thread-max", 6);
        overrides.put("max-reconsume-times", 99);
        overrides.put("consume-from-where", "invalid-value");
        overrides.put("message-model", "broadcasting");
        overrides.put("use-tls", true);
        overrides.put("vip-channel-enabled", true);
        overrides.put("access-channel", "CLOUD");

        DefaultMQPushConsumer consumer = factory.createPushConsumer("", overrides);

        assertEquals("override-consumer-group", consumer.getConsumerGroup());
        assertEquals("127.0.0.1:9876", consumer.getNamesrvAddr());
        assertEquals(8, consumer.getConsumeThreadMin());
        assertEquals(8, consumer.getConsumeThreadMax());
        assertEquals(99, consumer.getMaxReconsumeTimes());
        assertEquals(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET, consumer.getConsumeFromWhere());
        assertEquals(MessageModel.BROADCASTING, consumer.getMessageModel());
        assertEquals(AccessChannel.CLOUD, consumer.getAccessChannel());
        assertEquals(true, consumer.isUseTLS());
        assertEquals(true, consumer.isVipChannelEnabled());
    }

    @Test
    public void createPushConsumerUsesDefaultGroupWhenOverrideBlank() {
        RocketMqSettings settings = RocketMqSettings.fromConfig(new MapConfigView(Map.of(
                "rocketmq.enabled", true,
                "rocketmq.consumer.group", "default-consumer-group"
        )));
        RocketMqClientFactory factory = new RocketMqClientFactory(settings, getClass().getClassLoader());

        DefaultMQPushConsumer consumer = factory.createPushConsumer("  ", Map.of());

        assertEquals("default-consumer-group", consumer.getConsumerGroup());
    }
}
