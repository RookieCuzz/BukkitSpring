package com.cuzz.starter.bukkitspring.mybatis;

import com.cuzz.bukkitspring.api.StarterRegistry;
import com.cuzz.starter.bukkitspring.mybatis.autoconfigure.MybatisDependencies;
import java.util.logging.Logger;

/**
 * MyBatis starter entry class.
 *
 * <p>Registers runtime dependencies and scan packages on class load.
 * BukkitSpring loads this class during onLoad() and triggers the static block.
 *
 * <p>MANIFEST.MF entry:
 * <pre>
 * Starter-Class: com.cuzz.starter.bukkitspring.mybatis.MybatisStarter
 * </pre>
 */
public final class MybatisStarter {
    private static final Logger LOGGER = Logger.getLogger(MybatisStarter.class.getName());

    static {
        StarterRegistry.registerDependencies(MybatisDependencies.get());
        StarterRegistry.registerScanPackage("com.cuzz.starter.bukkitspring.mybatis");
        LOGGER.info("[MybatisStarter] Registered MyBatis dependencies and configuration package");
    }

    public MybatisStarter() {
    }
}
