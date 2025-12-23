package com.example.bukkitspringdemo;

import com.monstercontroller.bukkitspring.api.annotation.Autowired;
import com.monstercontroller.bukkitspring.api.annotation.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

@Component
public final class GreetingCommand implements CommandExecutor {
    private final GreetingService service;

    @Autowired
    public GreetingCommand(GreetingService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = args.length == 0 ? "World" : String.join(" ", args).trim();
        if (name.isEmpty()) {
            name = "World";
        }
        sender.sendMessage("[DEBUG] GreetingCommand invoked by " + sender.getName());
        sender.sendMessage(service.greet(name));
        sender.sendMessage("label=" + label.toLowerCase(Locale.ROOT));
        return true;
    }
}
