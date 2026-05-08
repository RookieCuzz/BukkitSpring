package top.skillstone.consumableskillbridge.engine;

import io.lumine.mythic.lib.MythicLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.skillstone.consumableskillbridge.ConsumableSkillBridgePlugin;
import top.skillstone.consumableskillbridge.model.ConsumableCommandSpec;
import top.skillstone.consumableskillbridge.model.ConsumableDefinition;

public class CommandEngine {
    private final ConsumableSkillBridgePlugin plugin;

    public CommandEngine(ConsumableSkillBridgePlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, ConsumableDefinition definition) {
        for (ConsumableCommandSpec command : definition.getCommands()) {
            if (Math.random() > command.getChance()) {
                continue;
            }

            Runnable task = () -> dispatch(player, command.getFormat(), command.isConsole(), command.isOp());
            if (command.getDelayTicks() > 0L) {
                Bukkit.getScheduler().runTaskLater(plugin, task, command.getDelayTicks());
            } else {
                task.run();
            }
        }
    }

    private void dispatch(Player player, String format, boolean console, boolean op) {
        String parsed = MythicLib.plugin.getPlaceholderParser().parse(player, format);
        String commandLine = parsed == null ? "" : parsed.trim();
        if (commandLine.startsWith("/")) {
            commandLine = commandLine.substring(1).trim();
        }
        if (commandLine.isEmpty()) {
            return;
        }
        if (!plugin.isCommandAllowed(commandLine)) {
            plugin.warn("Blocked non-whitelisted consumable command: " + commandLine);
            return;
        }

        if (console) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine);
            return;
        }

        if (op && !player.isOp()) {
            player.setOp(true);
            try {
                Bukkit.dispatchCommand(player, commandLine);
            } finally {
                player.setOp(false);
            }
            return;
        }

        Bukkit.dispatchCommand(player, commandLine);
    }
}
