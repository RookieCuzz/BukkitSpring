package com.example.bukkitspringdemo;

import com.monstercontroller.bukkitspring.api.annotation.Autowired;
import com.monstercontroller.bukkitspring.api.annotation.Component;
import jakarta.annotation.PostConstruct;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

@Component
public final class CommandRegistrar {
    private final JavaPlugin plugin;
    private final GreetingCommand greetingCommand;

    @Autowired
    public CommandRegistrar(JavaPlugin plugin, GreetingCommand greetingCommand) {
        this.plugin = plugin;
        this.greetingCommand = greetingCommand;
    }

    @PostConstruct
    public void register() {
        PluginCommand command = plugin.getCommand("hello");
        if (command != null) {
            command.setExecutor(greetingCommand);
            plugin.getLogger().info("[DEBUG] Registered /hello command executor");
        } else {
            plugin.getLogger().warning("Command 'hello' missing in plugin.yml");
        }
    }
}
