package com.monstercontroller.bukkitspring.api.kafka;

import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.Map;

public final class ConsumerInfo {
    public final String id;
    public final List<String> topics;
    public final String groupId;
    public final ConsumerState state;
    public final long messagesConsumed;
    public final long lastPollTime;
    public final Map<TopicPartition, Long> currentOffsets;

    public ConsumerInfo(String id,
                        List<String> topics,
                        String groupId,
                        ConsumerState state,
                        long messagesConsumed,
                        long lastPollTime,
                        Map<TopicPartition, Long> currentOffsets) {
        this.id = id;
        this.topics = topics;
        this.groupId = groupId;
        this.state = state;
        this.messagesConsumed = messagesConsumed;
        this.lastPollTime = lastPollTime;
        this.currentOffsets = currentOffsets;
    }
}
