package com.cuzz.starter.bukkitspring.redis.autoconfigure;

import com.cuzz.bukkitspring.api.annotation.Bean;
import com.cuzz.bukkitspring.api.annotation.Configuration;
import com.cuzz.starter.bukkitspring.redis.config.RedisSettings;
import com.cuzz.bukkitspring.spi.config.ConfigView;

import java.util.logging.Logger;

/**
 * Redis auto configuration.
 *
 * <p>Creates settings beans from BukkitSpring config.yml.
 */
@Configuration
public class RedisAutoConfiguration {

    /**
     * Create RedisSettings bean from BukkitSpring config.
     */
    @Bean
    public RedisSettings redisSettings(ConfigView config, Logger logger) {
        RedisSettings settings = RedisSettings.fromConfig(config);

        if (settings.enabled) {
            logger.info("[Redis] Enabled in " + settings.mode + " mode");
        } else {
            logger.info("[Redis] Disabled");
        }

        return settings;
    }
}
