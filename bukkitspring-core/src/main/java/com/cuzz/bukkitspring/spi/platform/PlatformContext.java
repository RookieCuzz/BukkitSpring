package com.cuzz.bukkitspring.spi.platform;

import com.cuzz.bukkitspring.spi.config.ConfigView;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public interface PlatformContext {
    Logger getLogger();

    ClassLoader getClassLoader();

    ConfigView getConfig();

    Path getDataDirectory();

    PluginResourceResolver getPluginResourceResolver();

    PlatformScheduler getScheduler();

    Map<Class<?>, Object> getBuiltinBeans();
}
