package com.cuzz.starter.bukkitspring.kafka.api;

/**
 * 消费者状态
 */
public enum ConsumerState {
    /**
     * 运行中
     */
    RUNNING,
    
    /**
     * 已暂停
     */
    PAUSED,
    
    /**
     * 已停止
     */
    STOPPED,
    
    /**
     * 启动中
     */
    STARTING,
    
    /**
     * 停止中
     */
    STOPPING
}
