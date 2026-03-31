package com.cuzz.bukkitspring.dependency;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

public final class BukkitDependencyAccess implements DependencyAccess {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Path librariesDir;

    public BukkitDependencyAccess(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
        this.librariesDir = plugin.getServer().getWorldContainer().toPath().resolve("libraries");
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Path getLibrariesDirectory() {
        return librariesDir;
    }

    @Override
    public ClassLoader getPrimaryClassLoader() {
        return plugin.getClass().getClassLoader();
    }

    @Override
    public ClassLoader getSecondaryClassLoader() {
        return plugin.getServer().getClass().getClassLoader();
    }
}
