package com.cuzz.starter.bukkitspring.mybatis.autoconfigure;

import com.cuzz.bukkitspring.api.annotation.Bean;
import com.cuzz.bukkitspring.api.annotation.Configuration;
import com.cuzz.starter.bukkitspring.mybatis.config.MybatisSettings;
import com.cuzz.bukkitspring.spi.config.ConfigView;

import java.util.logging.Logger;

/**
 * MyBatis auto configuration.
 *
 * <p>Creates settings beans from BukkitSpring config.yml.
 */
@Configuration
public class MybatisAutoConfiguration {

    @Bean
    public MybatisSettings mybatisSettings(ConfigView config, Logger logger) {
        MybatisSettings settings = MybatisSettings.fromConfig(config);
        if (settings.enabled) {
            logger.info("[MyBatis] Enabled");
        } else {
            logger.info("[MyBatis] Disabled");
        }
        return settings;
    }
}
