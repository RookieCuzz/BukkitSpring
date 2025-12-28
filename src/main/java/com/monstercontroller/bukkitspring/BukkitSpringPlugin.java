package com.monstercontroller.bukkitspring;

import com.monstercontroller.bukkitspring.dependency.BukkitSpringDependencies;
import com.monstercontroller.bukkitspring.dependency.DependencyDownloader;
import com.monstercontroller.bukkitspring.logging.LokiLogHandler;
import com.monstercontroller.bukkitspring.logging.LokiSettings;
import com.monstercontroller.bukkitspring.kafka.DefaultKafkaConsumerManager;
import com.monstercontroller.bukkitspring.kafka.DefaultKafkaService;
import com.monstercontroller.bukkitspring.kafka.KafkaSettings;
import com.monstercontroller.bukkitspring.redis.DefaultRedisService;
import com.monstercontroller.bukkitspring.redis.RedisSettings;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BukkitSpringPlugin extends JavaPlugin {
    private final List<Logger> lokiLoggers = new ArrayList<>();
    private LokiLogHandler lokiHandler;
    private DefaultKafkaService kafkaService;
    private DefaultKafkaConsumerManager consumerManager;
    private DefaultRedisService redisService;
    private boolean dependenciesReady = true;

    @Override
    public void onLoad() {
        dependenciesReady = ensureDependencies();
    }

    @Override
    public void onEnable() {
        if (!dependenciesReady) {
            return;
        }
        saveDefaultConfig();
        LokiSettings settings = LokiSettings.fromConfig(getConfig());
        if (settings.enabled) {
            if (settings.url == null || settings.url.isBlank()) {
                getLogger().warning("Loki is enabled but loki.url is empty.");
            } else {
                lokiHandler = new LokiLogHandler(settings);
                if (settings.useServerLogger) {
                    attachLogger(Bukkit.getServer().getLogger());
                }
                if (settings.useRootLogger) {
                    attachLogger(Logger.getLogger(""));
                }
            }
        }
        KafkaSettings kafkaSettings = KafkaSettings.fromConfig(getConfig());
        kafkaService = new DefaultKafkaService(kafkaSettings, getLogger());
        BukkitSpring.setKafkaService(kafkaService);
        consumerManager = new DefaultKafkaConsumerManager(kafkaService, kafkaSettings, getLogger());
        BukkitSpring.setKafkaConsumerManager(consumerManager);
        if (kafkaSettings.enabled) {
            getLogger().info("Kafka service enabled.");
        } else {
            getLogger().info("Kafka service disabled.");
        }
        RedisSettings redisSettings = RedisSettings.fromConfig(getConfig());
        redisService = new DefaultRedisService(redisSettings, getLogger());
        BukkitSpring.setRedisService(redisService);
        if (redisSettings.enabled) {
            getLogger().info("Redis service enabled.");
        } else {
            getLogger().info("Redis service disabled.");
        }
        getLogger().info("BukkitSpring enabled. Waiting for plugins to register.");
    }

    @Override
    public void onDisable() {
        for (Logger logger : lokiLoggers) {
            for (Handler handler : logger.getHandlers()) {
                if (handler == lokiHandler) {
                    logger.removeHandler(handler);
                }
            }
        }
        lokiLoggers.clear();
        if (lokiHandler != null) {
            lokiHandler.close();
            lokiHandler = null;
        }
        if (consumerManager != null) {
            consumerManager.stopAll();
            consumerManager = null;
            BukkitSpring.clearKafkaConsumerManager();
        }
        BukkitSpring.shutdownAll();
        if (kafkaService != null) {
            kafkaService.close();
            kafkaService = null;
            BukkitSpring.clearKafkaService();
        }
        if (redisService != null) {
            redisService.close();
            redisService = null;
            BukkitSpring.clearRedisService();
        }
    }

    private void attachLogger(Logger logger) {
        if (logger == null || lokiHandler == null) {
            return;
        }
        for (Handler handler : logger.getHandlers()) {
            if (handler == lokiHandler) {
                return;
            }
        }
        logger.addHandler(lokiHandler);
        lokiLoggers.add(logger);
    }

    private boolean ensureDependencies() {
        DependencyDownloader downloader = new DependencyDownloader(this);
        try {
            downloader.ensureDependencies(BukkitSpringDependencies.required());
            return true;
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Dependency download failed. Disabling plugin.", ex);
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
    }
}
