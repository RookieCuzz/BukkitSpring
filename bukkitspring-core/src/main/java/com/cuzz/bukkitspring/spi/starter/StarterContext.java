package com.cuzz.bukkitspring.spi.starter;

import com.cuzz.bukkitspring.spi.config.ConfigView;

import java.util.logging.Logger;

public interface StarterContext {
    Logger getLogger();

    ClassLoader getClassLoader();

    ConfigView getConfig();

    <T> void registerGlobalBean(Class<T> type, T instance);
}
