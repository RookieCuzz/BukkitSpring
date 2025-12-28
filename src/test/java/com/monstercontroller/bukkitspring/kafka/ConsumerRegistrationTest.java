package com.monstercontroller.bukkitspring.kafka;

import com.monstercontroller.bukkitspring.api.kafka.ConsumerRegistration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConsumerRegistrationTest {
    @Test
    public void buildDefaults() {
        ConsumerRegistration<String, String> registration = ConsumerRegistration.<String, String>builder()
                .topics("topic-a")
                .groupId("group-a")
                .handler(record -> {})
                .build();
        assertTrue(registration.autoStartup);
        assertEquals(1, registration.concurrency);
        assertTrue(registration.properties.isEmpty());
    }

    @Test
    public void requiresTopics() {
        assertThrows(IllegalArgumentException.class, () -> ConsumerRegistration.builder()
                .groupId("group-a")
                .handler(record -> {})
                .build());
    }

    @Test
    public void requiresGroupId() {
        assertThrows(IllegalArgumentException.class, () -> ConsumerRegistration.builder()
                .topics("topic-a")
                .handler(record -> {})
                .build());
    }

    @Test
    public void requiresHandler() {
        assertThrows(NullPointerException.class, () -> ConsumerRegistration.builder()
                .topics("topic-a")
                .groupId("group-a")
                .build());
    }

    @Test
    public void concurrencyMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> ConsumerRegistration.builder()
                .topics("topic-a")
                .groupId("group-a")
                .handler(record -> {})
                .concurrency(0));
    }
}
