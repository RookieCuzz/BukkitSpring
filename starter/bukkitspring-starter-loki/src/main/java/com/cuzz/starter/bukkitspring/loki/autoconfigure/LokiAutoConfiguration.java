package com.cuzz.starter.bukkitspring.loki.autoconfigure;

import com.cuzz.bukkitspring.api.annotation.Bean;
import com.cuzz.bukkitspring.api.annotation.Configuration;
import com.cuzz.starter.bukkitspring.loki.config.LokiSettings;
import com.cuzz.bukkitspring.spi.config.ConfigView;

/**
 * Loki 日志自动配置类
 * 提供 Loki 日志相关的 Bean 定义
 */
@Configuration
public class LokiAutoConfiguration {

    /**
     * 创建 Loki 设置 Bean
     * 
     * @param plugin Bukkit 插件实例
     * @return Loki 设置对象
     */
    @Bean
    public LokiSettings lokiSettings(ConfigView config) {
        return LokiSettings.fromConfig(config);
    }
}
