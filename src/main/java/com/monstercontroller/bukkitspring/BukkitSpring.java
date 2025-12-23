package com.monstercontroller.bukkitspring;

import com.monstercontroller.bukkitspring.api.ApplicationContext;
import com.monstercontroller.bukkitspring.internal.SimpleApplicationContext;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BukkitSpring {
    private static final Map<JavaPlugin, ApplicationContext> CONTEXTS = new ConcurrentHashMap<>();

    private BukkitSpring() {
    }

    public static synchronized ApplicationContext registerPlugin(JavaPlugin plugin, String... basePackages) {
        ApplicationContext existing = CONTEXTS.get(plugin);
        if (existing != null) {
            return existing;
        }
        SimpleApplicationContext context = new SimpleApplicationContext(plugin);
        if (basePackages != null && basePackages.length > 0) {
            context.scan(basePackages);
        }
        CONTEXTS.put(plugin, context);
        return context;
    }

    public static ApplicationContext getContext(JavaPlugin plugin) {
        return CONTEXTS.get(plugin);
    }

    public static void unregisterPlugin(JavaPlugin plugin) {
        ApplicationContext context = CONTEXTS.remove(plugin);
        if (context != null) {
            context.close();
        }
    }

    public static void shutdownAll() {
        for (ApplicationContext context : CONTEXTS.values()) {
            context.close();
        }
        CONTEXTS.clear();
    }
}
