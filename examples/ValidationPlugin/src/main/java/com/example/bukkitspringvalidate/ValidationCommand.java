package com.example.bukkitspringvalidate;

import com.monstercontroller.bukkitspring.api.annotation.Autowired;
import com.monstercontroller.bukkitspring.api.annotation.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

@Component
public final class ValidationCommand implements CommandExecutor {
    private final ValidationService validationService;

    @Autowired
    public ValidationCommand(ValidationService validationService) {
        this.validationService = validationService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("[DEBUG] bscheck invoked by " + sender.getName());
        for (String line : validationService.runChecks()) {
            sender.sendMessage(line);
        }
        return true;
    }
}
