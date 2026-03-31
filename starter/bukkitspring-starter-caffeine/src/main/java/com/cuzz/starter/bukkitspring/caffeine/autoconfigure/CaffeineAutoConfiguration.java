package com.cuzz.starter.bukkitspring.caffeine.autoconfigure;

import com.cuzz.bukkitspring.api.annotation.Bean;
import com.cuzz.bukkitspring.api.annotation.Configuration;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.starter.bukkitspring.caffeine.config.CaffeineSettings;

import java.util.logging.Logger;

/**
 * Caffeine auto configuration.
 */
@Configuration
public class CaffeineAutoConfiguration {

    /**
     * Create CaffeineSettings bean from BukkitSpring config.
     */
    @Bean
    public CaffeineSettings caffeineSettings(ConfigView config, Logger logger) {
        CaffeineSettings settings = CaffeineSettings.fromConfig(config);
        if (settings.enabled) {
            logger.info("[Caffeine] Enabled, default-cache=" + settings.defaultCacheName
                    + ", preconfigured-caches=" + settings.namedCacheSpecs.size());
        } else {
            logger.info("[Caffeine] Disabled");
        }
        return settings;
    }
}
