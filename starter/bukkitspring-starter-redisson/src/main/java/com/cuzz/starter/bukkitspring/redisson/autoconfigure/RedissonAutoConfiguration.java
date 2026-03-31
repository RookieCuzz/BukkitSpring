package com.cuzz.starter.bukkitspring.redisson.autoconfigure;

import com.cuzz.bukkitspring.api.annotation.Bean;
import com.cuzz.bukkitspring.api.annotation.Configuration;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.starter.bukkitspring.redisson.config.RedissonSettings;

import java.util.logging.Logger;

/**
 * Redisson auto configuration.
 */
@Configuration
public class RedissonAutoConfiguration {

    @Bean
    public RedissonSettings redissonSettings(ConfigView config, Logger logger) {
        RedissonSettings settings = RedissonSettings.fromConfig(config);
        if (settings.enabled) {
            logger.info("[Redisson] Enabled in " + settings.mode + " mode");
        } else {
            logger.info("[Redisson] Disabled");
        }
        return settings;
    }
}
