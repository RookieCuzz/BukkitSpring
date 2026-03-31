package com.cuzz.starter.bukkitspring.prometheus.autoconfigure;

import com.cuzz.bukkitspring.api.annotation.Bean;
import com.cuzz.bukkitspring.api.annotation.Configuration;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.starter.bukkitspring.prometheus.config.PrometheusSettings;

import java.util.logging.Logger;

/**
 * Prometheus auto configuration.
 */
@Configuration
public class PrometheusAutoConfiguration {

    /**
     * Create PrometheusSettings bean from BukkitSpring config.
     */
    @Bean
    public PrometheusSettings prometheusSettings(ConfigView config, Logger logger) {
        PrometheusSettings settings = PrometheusSettings.fromConfig(config);

        if (settings.enabled) {
            logger.info("[Prometheus] Enabled with pushgateway: " + settings.pushGatewayUrl);
        } else {
            logger.info("[Prometheus] Disabled");
        }

        return settings;
    }
}
