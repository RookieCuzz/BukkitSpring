package com.cuzz.starter.bukkitspring.rocketmq;

import com.cuzz.bukkitspring.api.StarterRegistry;
import com.cuzz.starter.bukkitspring.rocketmq.autoconfigure.RocketMqDependencies;

import java.util.logging.Logger;

/**
 * RocketMQ starter entry.
 *
 * <p>Registers runtime dependencies and scan packages on class load.
 */
public final class RocketMqStarter {
    private static final Logger LOGGER = Logger.getLogger(RocketMqStarter.class.getName());

    static {
        StarterRegistry.registerDependencies(RocketMqDependencies.get());
        StarterRegistry.registerScanPackage("com.cuzz.starter.bukkitspring.rocketmq");
        LOGGER.info("[RocketMqStarter] Registered RocketMQ dependencies and configuration package");
    }

    public RocketMqStarter() {
    }
}
