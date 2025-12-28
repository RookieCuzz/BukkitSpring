package com.monstercontroller.bukkitspring.api.kafka;

public final class ConsumerStatus {
    public final String id;
    public final ConsumerState state;
    public final long messagesConsumed;
    public final long lastPollTime;

    public ConsumerStatus(String id, ConsumerState state, long messagesConsumed, long lastPollTime) {
        this.id = id;
        this.state = state;
        this.messagesConsumed = messagesConsumed;
        this.lastPollTime = lastPollTime;
    }
}
