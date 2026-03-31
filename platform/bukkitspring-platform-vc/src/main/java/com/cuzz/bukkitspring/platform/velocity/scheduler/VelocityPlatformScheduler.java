package com.cuzz.bukkitspring.platform.velocity.scheduler;

import com.cuzz.bukkitspring.platform.velocity.VelocitySpringPlugin;
import com.cuzz.bukkitspring.spi.platform.PlatformScheduler;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.Objects;

public final class VelocityPlatformScheduler implements PlatformScheduler {
    private final VelocitySpringPlugin plugin;
    private final ProxyServer server;

    public VelocityPlatformScheduler(VelocitySpringPlugin plugin, ProxyServer server) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public void runSync(Runnable task) {
        if (task == null) {
            return;
        }
        task.run();
    }

    @Override
    public void runAsync(Runnable task) {
        if (task == null) {
            return;
        }
        server.getScheduler().buildTask(plugin, task).schedule();
    }
}
