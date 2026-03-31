package com.cuzz.starter.bukkitspring.kafka;

import com.cuzz.starter.bukkitspring.kafka.api.ConsumerRegistration;
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
        assertTrue(registration.isAutoStartup());
        assertEquals(1, registration.getConcurrency());
        assertTrue(registration.getProperties().isEmpty());
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
        assertThrows(NullPointerException.class, () -> ConsumerRegistration.builder()
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
