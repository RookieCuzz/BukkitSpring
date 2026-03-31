package com.cuzz.starter.bukkitspring.mybatis.internal.mapper;

import com.cuzz.bukkitspring.spi.platform.PluginResource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class MapperResourceResolver {
    public InputStream openMapperResource(PluginResource plugin, String resourcePath) throws Exception {
        Path dataDir = plugin.getDataDirectory();
        if (dataDir != null && resourcePath != null && !resourcePath.isBlank()) {
            Path direct = dataDir.resolve(resourcePath);
            if (Files.exists(direct)) {
                return Files.newInputStream(direct, StandardOpenOption.READ);
            }
            Path underMappers = dataDir.resolve("mappers").resolve(resourcePath);
            if (Files.exists(underMappers)) {
                return Files.newInputStream(underMappers, StandardOpenOption.READ);
            }
        }
        return plugin.openResource(resourcePath);
    }
}
