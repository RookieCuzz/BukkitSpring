package com.cuzz.starter.bukkitspring.kafka.api;

/**
 * 消费者错误处理策略
 */
public enum ConsumerErrorHandlingStrategy {
    /**
     * 跳过错误消息，继续处理下一条
     */
    SKIP,
    
    /**
     * 重试失败的消息
     */
    RETRY,
    
    /**
     * 发送到死信队列
     */
    DLQ,
    
    /**
     * 停止消费者
     */
    STOP;
    
    /**
     * 从字符串解析策略
     */
    public static ConsumerErrorHandlingStrategy fromString(String value) {
        if (value == null || value.isBlank()) {
            return SKIP;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SKIP;
        }
    }
}
