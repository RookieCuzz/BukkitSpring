package com.cuzz.starter.bukkitspring.kafka.api;

import java.util.*;

/**
 * 消费者注册信息
 * 
 * @param <K> 消息 key 的类型
 * @param <V> 消息 value 的类型
 */
public final class ConsumerRegistration<K, V> {
    private final String id;
    private final List<String> topics;
    private final String groupId;
    private final KafkaMessageHandler<K, V> handler;
    private final int concurrency;
    private final boolean autoStartup;
    private final Map<String, Object> properties;
    
    private ConsumerRegistration(Builder<K, V> builder) {
        this.id = builder.id;
        this.topics = Collections.unmodifiableList(new ArrayList<>(builder.topics));
        this.groupId = Objects.requireNonNull(builder.groupId, "groupId");
        this.handler = Objects.requireNonNull(builder.handler, "handler");
        this.concurrency = builder.concurrency;
        this.autoStartup = builder.autoStartup;
        this.properties = builder.properties != null ? 
                Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties)) : 
                Collections.emptyMap();
    }
    
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }
    
    public String getId() {
        return id;
    }
    
    public List<String> getTopics() {
        return topics;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public KafkaMessageHandler<K, V> getHandler() {
        return handler;
    }
    
    public int getConcurrency() {
        return concurrency;
    }
    
    public boolean isAutoStartup() {
        return autoStartup;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public static class Builder<K, V> {
        private String id;
        private List<String> topics = new ArrayList<>();
        private String groupId;
        private KafkaMessageHandler<K, V> handler;
        private int concurrency = 1;
        private boolean autoStartup = true;
        private Map<String, Object> properties;
        
        public Builder<K, V> id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder<K, V> topics(String... topics) {
            if (topics != null && topics.length > 0) {
                this.topics.addAll(Arrays.asList(topics));
            }
            return this;
        }
        
        public Builder<K, V> topics(List<String> topics) {
            if (topics != null) {
                this.topics.addAll(topics);
            }
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
            this.properties = properties;
            return this;
        }
        
        public ConsumerRegistration<K, V> build() {
            if (topics.isEmpty()) {
                throw new IllegalArgumentException("topics cannot be empty");
            }
            return new ConsumerRegistration<>(this);
        }
    }
}
