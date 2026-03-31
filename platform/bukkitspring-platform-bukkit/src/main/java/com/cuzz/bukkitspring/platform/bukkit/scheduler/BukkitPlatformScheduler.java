package com.cuzz.bukkitspring.platform.bukkit.scheduler;

import com.cuzz.bukkitspring.spi.platform.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Objects;

public final class BukkitPlatformScheduler implements PlatformScheduler {
    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;

    public BukkitPlatformScheduler(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = plugin.getServer().getScheduler();
    }

    @Override
    public void runSync(Runnable task) {
        if (task == null) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            scheduler.runTask(plugin, task);
        }
    }

    @Override
    public void runAsync(Runnable task) {
        if (task == null) {
            return;
        }
        scheduler.runTaskAsynchronously(plugin, task);
    }
}
