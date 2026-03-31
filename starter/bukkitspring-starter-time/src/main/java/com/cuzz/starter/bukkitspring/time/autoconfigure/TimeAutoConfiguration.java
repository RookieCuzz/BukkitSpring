package com.cuzz.starter.bukkitspring.time.autoconfigure;

import com.cuzz.bukkitspring.api.annotation.Bean;
import com.cuzz.bukkitspring.api.annotation.Configuration;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.starter.bukkitspring.time.config.TimeSettings;

import java.util.logging.Logger;

/**
 * Time auto configuration.
 */
@Configuration
public class TimeAutoConfiguration {

    /**
     * Create TimeSettings bean from BukkitSpring config.
     */
    @Bean
    public TimeSettings timeSettings(ConfigView config, Logger logger) {
        TimeSettings settings = TimeSettings.fromConfig(config);
        if (settings.enabled) {
            logger.info("[Time] Enabled with zone: " + settings.zoneId);
            if (settings.allowSetSystemTime) {
                logger.warning("[Time] System time adjustment is enabled for debugging.");
            }
        } else {
            logger.info("[Time] Disabled");
        }
        return settings;
    }
}
