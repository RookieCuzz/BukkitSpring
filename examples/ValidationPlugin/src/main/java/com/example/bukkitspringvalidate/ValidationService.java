package com.example.bukkitspringvalidate;

import com.monstercontroller.bukkitspring.api.Provider;
import com.monstercontroller.bukkitspring.api.annotation.Autowired;
import com.monstercontroller.bukkitspring.api.annotation.Component;
import com.monstercontroller.bukkitspring.api.annotation.Qualifier;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

@Component
public final class ValidationService {
    private final JavaPlugin plugin;
    private final Storage defaultStorage;
    private final Storage fastStorage;
    private final Provider<TempObject> tempProvider;
    private volatile boolean initialized;

    @Autowired
    public ValidationService(
            JavaPlugin plugin,
            Storage defaultStorage,
            @Qualifier("fastStorage") Storage fastStorage,
            Provider<TempObject> tempProvider
    ) {
        this.plugin = plugin;
        this.defaultStorage = defaultStorage;
        this.fastStorage = fastStorage;
        this.tempProvider = tempProvider;
    }

    @PostConstruct
    public void init() {
        initialized = true;
        plugin.getLogger().info("[DEBUG] @PostConstruct invoked");
    }

    @PreDestroy
    public void shutdown() {
        plugin.getLogger().info("[DEBUG] @PreDestroy invoked");
    }

    public List<String> runChecks() {
        List<String> lines = new ArrayList<>();
        plugin.getLogger().info("[DEBUG] Running validation checks");
        TempObject first = tempProvider.get();
        TempObject second = tempProvider.get();
        boolean prototypeOk = first.getId() != second.getId();
        lines.add("BukkitSpring validation:");
        lines.add("1) @Primary injection: " + defaultStorage.name());
        lines.add("2) @Qualifier injection: " + fastStorage.name());
        lines.add("3) Provider + @Scope(PROTOTYPE): " + (prototypeOk ? "OK" : "FAIL")
                + " (" + first.getId() + "," + second.getId() + ")");
        lines.add("4) @PostConstruct executed: " + (initialized ? "OK" : "FAIL"));
        return lines;
    }
}
