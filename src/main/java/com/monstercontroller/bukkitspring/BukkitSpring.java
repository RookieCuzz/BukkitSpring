package com.monstercontroller.bukkitspring;

import com.monstercontroller.bukkitspring.api.ApplicationContext;
import com.monstercontroller.bukkitspring.api.kafka.KafkaService;
import com.monstercontroller.bukkitspring.api.kafka.KafkaConsumerManager;
import com.monstercontroller.bukkitspring.api.redis.RedisService;
import com.monstercontroller.bukkitspring.internal.SimpleApplicationContext;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BukkitSpring {
    private static final Map<JavaPlugin, ApplicationContext> CONTEXTS = new ConcurrentHashMap<>();
    private static volatile KafkaService kafkaService;
    private static volatile KafkaConsumerManager kafkaConsumerManager;
    private static volatile RedisService redisService;

    private BukkitSpring() {
    }

    public static synchronized ApplicationContext registerPlugin(JavaPlugin plugin, String... basePackages) {
        ApplicationContext existing = CONTEXTS.get(plugin);
        if (existing != null) {
            return existing;
        }
        SimpleApplicationContext context = new SimpleApplicationContext(plugin);
        if (basePackages != null && basePackages.length > 0) {
            context.scan(basePackages);
        }
        CONTEXTS.put(plugin, context);
        return context;
    }

    public static ApplicationContext getContext(JavaPlugin plugin) {
        return CONTEXTS.get(plugin);
    }

    public static KafkaService getKafkaService() {
        return kafkaService;
    }

    public static KafkaConsumerManager getKafkaConsumerManager() {
        return kafkaConsumerManager;
    }

    public static RedisService getRedisService() {
        return redisService;
    }

    public static void unregisterPlugin(JavaPlugin plugin) {
        ApplicationContext context = CONTEXTS.remove(plugin);
        if (context != null) {
            context.close();
        }
    }

    public static void shutdownAll() {
        for (ApplicationContext context : CONTEXTS.values()) {
            context.close();
        }
        CONTEXTS.clear();
    }

    static void setKafkaService(KafkaService service) {
        kafkaService = service;
    }

    static void clearKafkaService() {
        kafkaService = null;
    }

    static void setKafkaConsumerManager(KafkaConsumerManager manager) {
        kafkaConsumerManager = manager;
    }

    static void clearKafkaConsumerManager() {
        kafkaConsumerManager = null;
    }

    static void setRedisService(RedisService service) {
        redisService = service;
    }

    static void clearRedisService() {
        redisService = null;
    }
}
