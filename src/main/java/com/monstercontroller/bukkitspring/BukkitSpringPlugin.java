package com.monstercontroller.bukkitspring;

import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitSpringPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("BukkitSpring enabled. Waiting for plugins to register.");
    }

    @Override
    public void onDisable() {
        BukkitSpring.shutdownAll();
    }
}
