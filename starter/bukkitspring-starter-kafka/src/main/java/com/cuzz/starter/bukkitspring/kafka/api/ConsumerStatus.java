package com.cuzz.starter.bukkitspring.kafka.api;

import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 消费者状态信息
 */
public final class ConsumerStatus {
    private final String id;
    private final List<String> topics;
    private final String groupId;
    private final ConsumerState state;
    private final long messagesConsumed;
    private final long lastPollTime;
    private final Map<TopicPartition, Long> currentOffsets;
    private final int concurrency;
    
    public ConsumerStatus(String id, 
                          List<String> topics, 
                          String groupId, 
                          ConsumerState state,
                          long messagesConsumed, 
                          long lastPollTime, 
                          Map<TopicPartition, Long> currentOffsets,
                          int concurrency) {
        this.id = id;
        this.topics = Collections.unmodifiableList(topics);
        this.groupId = groupId;
        this.state = state;
        this.messagesConsumed = messagesConsumed;
        this.lastPollTime = lastPollTime;
        this.currentOffsets = currentOffsets != null ? Collections.unmodifiableMap(currentOffsets) : Collections.emptyMap();
        this.concurrency = concurrency;
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
    
    public long getMessagesConsumed() {
        return messagesConsumed;
    }
    
    public long getLastPollTime() {
        return lastPollTime;
    }
    
    public Map<TopicPartition, Long> getCurrentOffsets() {
        return currentOffsets;
    }
    
    public int getConcurrency() {
        return concurrency;
    }
    
    @Override
    public String toString() {
        return String.format("ConsumerStatus[id=%s, topics=%s, groupId=%s, state=%s, messagesConsumed=%d, concurrency=%d]",
                id, topics, groupId, state, messagesConsumed, concurrency);
    }
}
