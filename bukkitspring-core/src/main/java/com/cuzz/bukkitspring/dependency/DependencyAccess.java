package com.cuzz.bukkitspring.dependency;

import java.nio.file.Path;
import java.util.logging.Logger;

public interface DependencyAccess {
    Logger getLogger();

    Path getLibrariesDirectory();

    ClassLoader getPrimaryClassLoader();

    default ClassLoader getSecondaryClassLoader() {
        return null;
    }
}
