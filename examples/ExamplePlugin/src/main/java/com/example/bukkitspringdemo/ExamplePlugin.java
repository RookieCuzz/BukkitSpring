package com.example.bukkitspringdemo;

import com.monstercontroller.bukkitspring.BukkitSpring;
import com.monstercontroller.bukkitspring.api.ApplicationContext;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExamplePlugin extends JavaPlugin {
    private ApplicationContext context;

    @Override
    public void onLoad() {
        getLogger().info("[DEBUG] onLoad: registering BukkitSpring context");
        context = BukkitSpring.registerPlugin(this, "com.example.bukkitspringdemo");
    }

    @Override
    public void onEnable() {
        getLogger().info("[DEBUG] onEnable: refreshing BukkitSpring context");
        context.refresh();
        getLogger().info("[DEBUG] onEnable: context refreshed");
    }

    @Override
    public void onDisable() {
        getLogger().info("[DEBUG] onDisable: unregistering BukkitSpring context");
        BukkitSpring.unregisterPlugin(this);
    }
}
