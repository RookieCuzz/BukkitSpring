package com.cuzz.starter.bukkitspring.kafka.api;

import java.util.Collections;
import java.util.List;

/**
 * 消费者基本信息
 */
public final class ConsumerInfo {
    private final String id;
    private final List<String> topics;
    private final String groupId;
    private final ConsumerState state;
    private final int concurrency;
    private final boolean autoStartup;
    
    public ConsumerInfo(String id, 
                        List<String> topics, 
                        String groupId, 
                        ConsumerState state,
                        int concurrency,
                        boolean autoStartup) {
        this.id = id;
        this.topics = Collections.unmodifiableList(topics);
        this.groupId = groupId;
        this.state = state;
        this.concurrency = concurrency;
        this.autoStartup = autoStartup;
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
    
    public ConsumerState getState() {
        return state;
    }
    
    public int getConcurrency() {
        return concurrency;
    }
    
    public boolean isAutoStartup() {
        return autoStartup;
    }
    
    @Override
    public String toString() {
        return String.format("ConsumerInfo[id=%s, topics=%s, groupId=%s, state=%s, concurrency=%d, autoStartup=%s]",
                id, topics, groupId, state, concurrency, autoStartup);
    }
}
