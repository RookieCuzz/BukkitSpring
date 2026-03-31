package com.cuzz.bukkitspring.platform.bukkit.resource;

import com.cuzz.bukkitspring.spi.platform.PluginResource;
import com.cuzz.bukkitspring.spi.platform.PluginResourceResolver;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BukkitPluginResourceResolver implements PluginResourceResolver {
    private final PluginManager pluginManager;

    public BukkitPluginResourceResolver(PluginManager pluginManager) {
        this.pluginManager = Objects.requireNonNull(pluginManager, "pluginManager");
    }

    @Override
    public List<PluginResource> listPlugins() {
        Plugin[] plugins = pluginManager.getPlugins();
        List<PluginResource> resources = new ArrayList<>(plugins.length);
        for (Plugin plugin : plugins) {
            resources.add(new BukkitPluginResource(plugin));
        }
        return resources;
    }

    private static final class BukkitPluginResource implements PluginResource {
        private final Plugin plugin;

        private BukkitPluginResource(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public Object getKey() {
            return plugin;
        }

        @Override
        public String getName() {
            return plugin.getName();
        }

        @Override
        public ClassLoader getClassLoader() {
            return plugin.getClass().getClassLoader();
        }

        @Override
        public String getMainPackage() {
            return plugin.getClass().getPackageName();
        }

        @Override
        public Path getSourcePath() {
            try {
                URL url = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
                if (url == null) {
                    return null;
                }
                return Paths.get(url.toURI());
            } catch (Exception ex) {
                return null;
            }
        }

        @Override
        public InputStream openResource(String path) {
            return plugin.getResource(path);
        }

        @Override
        public Path getDataDirectory() {
            return plugin.getDataFolder().toPath();
        }
    }
}
