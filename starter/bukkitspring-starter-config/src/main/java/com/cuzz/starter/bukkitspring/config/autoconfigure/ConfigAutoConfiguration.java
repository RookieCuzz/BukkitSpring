package com.cuzz.starter.bukkitspring.config.autoconfigure;

import com.cuzz.bukkitspring.api.annotation.Bean;
import com.cuzz.bukkitspring.api.annotation.Configuration;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.starter.bukkitspring.config.config.ConfigStarterSettings;

import java.util.logging.Logger;

/**
 * Config starter auto configuration.
 */
@Configuration
public class ConfigAutoConfiguration {

    @Bean
    public ConfigStarterSettings configStarterSettings(ConfigView config, Logger logger) {
        ConfigStarterSettings settings = ConfigStarterSettings.fromConfig(config);
        if (settings.enabled) {
            logger.info("[ConfigStarter] Enabled. strategy=" + settings.defaultStrategy
                    + ", local=" + settings.localEnabled
                    + ", remote=" + settings.remoteEnabled);
        } else {
            logger.info("[ConfigStarter] Disabled");
        }
        return settings;
    }
}
