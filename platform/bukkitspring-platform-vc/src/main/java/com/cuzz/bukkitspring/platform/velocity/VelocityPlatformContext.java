package com.cuzz.bukkitspring.platform.velocity;

import com.cuzz.bukkitspring.platform.velocity.resource.VelocityPluginResourceResolver;
import com.cuzz.bukkitspring.platform.velocity.scheduler.VelocityPlatformScheduler;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.bukkitspring.spi.platform.PlatformContext;
import com.cuzz.bukkitspring.spi.platform.PlatformScheduler;
import com.cuzz.bukkitspring.spi.platform.PluginResourceResolver;
import com.velocitypowered.api.proxy.ProxyServer;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class VelocityPlatformContext implements PlatformContext {
    private final VelocitySpringPlugin plugin;
    private final ProxyServer server;
    private final org.slf4j.Logger slf4jLogger;
    private final Logger julLogger;
    private final ConfigView configView;
    private final Path dataDirectory;
    private final PluginResourceResolver pluginResourceResolver;
    private final PlatformScheduler scheduler;
    private final Map<Class<?>, Object> builtinBeans = new LinkedHashMap<>();

    public VelocityPlatformContext(VelocitySpringPlugin plugin,
                                   ProxyServer server,
                                   org.slf4j.Logger slf4jLogger,
                                   Logger julLogger,
                                   ConfigView configView,
                                   Path dataDirectory) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.server = Objects.requireNonNull(server, "server");
        this.slf4jLogger = Objects.requireNonNull(slf4jLogger, "slf4jLogger");
        this.julLogger = Objects.requireNonNull(julLogger, "julLogger");
        this.configView = Objects.requireNonNull(configView, "configView");
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.pluginResourceResolver = new VelocityPluginResourceResolver(server);
        this.scheduler = new VelocityPlatformScheduler(plugin, server);
        registerBuiltins();
    }

    @Override
    public Logger getLogger() {
        return julLogger;
    }

    @Override
    public ClassLoader getClassLoader() {
        return plugin.getClass().getClassLoader();
    }

    @Override
    public ConfigView getConfig() {
        return configView;
    }

    @Override
    public Path getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public PluginResourceResolver getPluginResourceResolver() {
        return pluginResourceResolver;
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public Map<Class<?>, Object> getBuiltinBeans() {
        return Map.copyOf(builtinBeans);
    }

    private void registerBuiltins() {
        builtinBeans.put(VelocitySpringPlugin.class, plugin);
        builtinBeans.put(ProxyServer.class, server);
        builtinBeans.put(org.slf4j.Logger.class, slf4jLogger);
        builtinBeans.put(Path.class, dataDirectory);
    }
}
