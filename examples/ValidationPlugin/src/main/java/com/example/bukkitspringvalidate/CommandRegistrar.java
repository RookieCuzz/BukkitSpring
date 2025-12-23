package com.example.bukkitspringvalidate;

import com.monstercontroller.bukkitspring.api.annotation.Autowired;
import com.monstercontroller.bukkitspring.api.annotation.Component;
import jakarta.annotation.PostConstruct;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

@Component
public final class CommandRegistrar {
    private final JavaPlugin plugin;
    private final ValidationCommand validationCommand;

    @Autowired
    public CommandRegistrar(JavaPlugin plugin, ValidationCommand validationCommand) {
        this.plugin = plugin;
        this.validationCommand = validationCommand;
    }

    @PostConstruct
    public void register() {
        PluginCommand command = plugin.getCommand("bscheck");
        if (command != null) {
            command.setExecutor(validationCommand);
            plugin.getLogger().info("[DEBUG] Registered /bscheck command executor");
        } else {
            plugin.getLogger().warning("Command 'bscheck' missing in plugin.yml");
        }
    }
}
