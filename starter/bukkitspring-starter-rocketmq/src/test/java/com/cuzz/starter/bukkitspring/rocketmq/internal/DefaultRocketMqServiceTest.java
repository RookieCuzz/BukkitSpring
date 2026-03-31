package com.cuzz.starter.bukkitspring.rocketmq.internal;

import com.cuzz.starter.bukkitspring.rocketmq.config.RocketMqSettings;
import com.cuzz.starter.bukkitspring.rocketmq.testutil.MapConfigView;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultRocketMqServiceTest {

    @Test
    public void disabledServiceRejectsOperations() {
        RocketMqSettings settings = RocketMqSettings.fromConfig(new MapConfigView(Map.of(
                "rocketmq.enabled", false
        )));
        DefaultRocketMqService service = new DefaultRocketMqService(settings, Logger.getLogger("test"));

        assertFalse(service.isEnabled());
        assertThrows(IllegalStateException.class, service::defaultProducer);
        assertThrows(IllegalStateException.class, () -> service.createProducer(Map.of()));
        assertThrows(IllegalStateException.class, () -> service.createPushConsumer("group-a", Map.of()));
        assertThrows(IllegalStateException.class, () -> service.send("topic-a", "payload"));
    }

    @Test
    public void closeIsIdempotentAndDisablesService() {
        RocketMqSettings settings = RocketMqSettings.fromConfig(new MapConfigView(Map.of(
                "rocketmq.enabled", true
        )));
        DefaultRocketMqService service = new DefaultRocketMqService(settings, Logger.getLogger("test"));

        assertTrue(service.isEnabled());
        service.close();
        assertFalse(service.isEnabled());
        service.close();
    }

    @Test
    public void sendAndSendAsyncBuildExpectedMessages() throws Exception {
        RocketMqSettings settings = RocketMqSettings.fromConfig(new MapConfigView(Map.of(
                "rocketmq.enabled", true
        )));
        DefaultRocketMqService service = new DefaultRocketMqService(settings, Logger.getLogger("test"));
        TestProducer producer = new TestProducer();
        setField(service, "defaultProducer", producer);

        SendResult syncResult = service.send("topic-a", "tag-a", "hello");
        assertNotNull(syncResult);
        assertNotNull(producer.lastSyncMessage);
        assertEquals("topic-a", producer.lastSyncMessage.getTopic());
        assertEquals("tag-a", producer.lastSyncMessage.getTags());
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), producer.lastSyncMessage.getBody());

        CompletableFuture<SendResult> async = service.sendAsync("topic-b", "tag-b", "world");
        SendResult asyncResult = async.get(3, TimeUnit.SECONDS);
        assertNotNull(asyncResult);
        assertNotNull(producer.lastAsyncMessage);
        assertEquals("topic-b", producer.lastAsyncMessage.getTopic());
        assertEquals("tag-b", producer.lastAsyncMessage.getTags());
        assertArrayEquals("world".getBytes(StandardCharsets.UTF_8), producer.lastAsyncMessage.getBody());

        assertThrows(IllegalArgumentException.class, () -> service.send("   ", "payload"));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class TestProducer extends DefaultMQProducer {
        private Message lastSyncMessage;
        private Message lastAsyncMessage;

        private TestProducer() {
            super("test-producer-group");
        }

        @Override
        public SendResult send(Message message) {
            this.lastSyncMessage = message;
            SendResult result = new SendResult();
            result.setSendStatus(SendStatus.SEND_OK);
            result.setMsgId("sync-msg-id");
            return result;
        }

        @Override
        public void send(Message message, SendCallback sendCallback) {
            this.lastAsyncMessage = message;
            if (sendCallback != null) {
                SendResult result = new SendResult();
                result.setSendStatus(SendStatus.SEND_OK);
                result.setMsgId("async-msg-id");
                sendCallback.onSuccess(result);
            }
        }
    }
}
