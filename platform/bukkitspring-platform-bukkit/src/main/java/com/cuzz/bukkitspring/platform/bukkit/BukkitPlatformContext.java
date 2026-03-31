package com.cuzz.bukkitspring.platform.bukkit;

import com.cuzz.bukkitspring.config.BukkitConfigView;
import com.cuzz.bukkitspring.platform.bukkit.resource.BukkitPluginResourceResolver;
import com.cuzz.bukkitspring.platform.bukkit.scheduler.BukkitPlatformScheduler;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.bukkitspring.spi.platform.PlatformContext;
import com.cuzz.bukkitspring.spi.platform.PlatformScheduler;
import com.cuzz.bukkitspring.spi.platform.PluginResourceResolver;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class BukkitPlatformContext implements PlatformContext {
    private final JavaPlugin plugin;
    private final ConfigView configView;
    private final Path dataDirectory;
    private final PluginResourceResolver pluginResourceResolver;
    private final PlatformScheduler scheduler;
    private final Map<Class<?>, Object> builtinBeans = new LinkedHashMap<>();

    public BukkitPlatformContext(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configView = new BukkitConfigView(plugin.getConfig());
        this.dataDirectory = plugin.getDataFolder().toPath();
        this.pluginResourceResolver = new BukkitPluginResourceResolver(plugin.getServer().getPluginManager());
        this.scheduler = new BukkitPlatformScheduler(plugin);
        registerBuiltins();
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
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
        builtinBeans.put(JavaPlugin.class, plugin);
        builtinBeans.put(Plugin.class, plugin);
        builtinBeans.put(Logger.class, plugin.getLogger());
        builtinBeans.put(Server.class, plugin.getServer());
        builtinBeans.put(PluginManager.class, plugin.getServer().getPluginManager());
        builtinBeans.put(FileConfiguration.class, plugin.getConfig());
        builtinBeans.put(BukkitScheduler.class, plugin.getServer().getScheduler());
    }
}
