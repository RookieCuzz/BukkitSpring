package com.cuzz.bukkitspring.dependency;

import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

public final class VelocityDependencyAccess implements DependencyAccess {
    private final Logger logger;
    private final Path librariesDir;
    private final ClassLoader classLoader;

    public VelocityDependencyAccess(Path librariesDir, Logger logger, ClassLoader classLoader) {
        this.librariesDir = Objects.requireNonNull(librariesDir, "librariesDir");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Path getLibrariesDirectory() {
        return librariesDir;
    }

    @Override
    public ClassLoader getPrimaryClassLoader() {
        return classLoader;
    }
}
