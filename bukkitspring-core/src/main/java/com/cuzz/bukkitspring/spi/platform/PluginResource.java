package com.cuzz.bukkitspring.spi.platform;

import java.io.InputStream;
import java.nio.file.Path;

public interface PluginResource {
    Object getKey();

    String getName();

    ClassLoader getClassLoader();

    String getMainPackage();

    Path getSourcePath();

    InputStream openResource(String path);

    default Path getDataDirectory() {
        return null;
    }
}
