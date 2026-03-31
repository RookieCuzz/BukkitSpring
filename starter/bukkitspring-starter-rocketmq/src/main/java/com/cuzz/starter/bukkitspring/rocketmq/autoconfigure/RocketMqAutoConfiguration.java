package com.cuzz.starter.bukkitspring.rocketmq.autoconfigure;

import com.cuzz.bukkitspring.api.annotation.Bean;
import com.cuzz.bukkitspring.api.annotation.Configuration;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.starter.bukkitspring.rocketmq.config.RocketMqSettings;

import java.util.logging.Logger;

/**
 * RocketMQ auto configuration.
 */
@Configuration
public class RocketMqAutoConfiguration {

    /**
     * Create RocketMqSettings bean from BukkitSpring config.
     */
    @Bean
    public RocketMqSettings rocketMqSettings(ConfigView config, Logger logger) {
        RocketMqSettings settings = RocketMqSettings.fromConfig(config);

        if (settings.enabled) {
            logger.info("[RocketMQ] Enabled with namesrv: " + settings.namesrvAddr);
        } else {
            logger.info("[RocketMQ] Disabled");
        }

        return settings;
    }
}
