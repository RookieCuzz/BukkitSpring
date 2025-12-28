package com.monstercontroller.bukkitspring.api.kafka;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ConsumerRegistration<K, V> {
    public final String id;
    public final List<String> topics;
    public final String groupId;
    public final KafkaMessageHandler<K, V> handler;
    public final int concurrency;
    public final boolean autoStartup;
    public final Map<String, Object> properties;

    private ConsumerRegistration(String id,
                                 List<String> topics,
                                 String groupId,
                                 KafkaMessageHandler<K, V> handler,
                                 int concurrency,
                                 boolean autoStartup,
                                 Map<String, Object> properties) {
        this.id = id;
        this.topics = topics;
        this.groupId = groupId;
        this.handler = handler;
        this.concurrency = concurrency;
        this.autoStartup = autoStartup;
        this.properties = properties;
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    public static final class Builder<K, V> {
        private String id;
        private List<String> topics;
        private String groupId;
        private KafkaMessageHandler<K, V> handler;
        private int concurrency = 1;
        private boolean autoStartup = true;
        private Map<String, Object> properties = Collections.emptyMap();

        public Builder<K, V> id(String id) {
            this.id = id;
            return this;
        }

        public Builder<K, V> topics(String... topics) {
            if (topics == null || topics.length == 0) {
                throw new IllegalArgumentException("topics must not be empty");
            }
            this.topics = List.of(topics);
            return this;
        }

        public Builder<K, V> groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder<K, V> handler(KafkaMessageHandler<K, V> handler) {
            this.handler = handler;
            return this;
        }

        public Builder<K, V> concurrency(int concurrency) {
            if (concurrency < 1) {
                throw new IllegalArgumentException("concurrency must be >= 1");
            }
            this.concurrency = concurrency;
            return this;
        }

        public Builder<K, V> autoStartup(boolean autoStartup) {
            this.autoStartup = autoStartup;
            return this;
        }

        public Builder<K, V> properties(Map<String, Object> properties) {
            if (properties == null || properties.isEmpty()) {
                this.properties = Collections.emptyMap();
            } else {
                this.properties = new LinkedHashMap<>(properties);
            }
            return this;
        }

        public ConsumerRegistration<K, V> build() {
            if (topics == null || topics.isEmpty()) {
                throw new IllegalArgumentException("topics must not be empty");
            }
            if (groupId == null || groupId.isBlank()) {
                throw new IllegalArgumentException("groupId must not be empty");
            }
            Objects.requireNonNull(handler, "handler");
            Map<String, Object> props = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            return new ConsumerRegistration<>(
                    id == null ? "" : id.trim(),
                    List.copyOf(topics),
                    groupId.trim(),
                    handler,
                    concurrency,
                    autoStartup,
                    props
            );
        }
    }
}
