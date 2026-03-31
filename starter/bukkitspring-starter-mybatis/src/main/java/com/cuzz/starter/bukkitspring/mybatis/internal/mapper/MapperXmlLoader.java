package com.cuzz.starter.bukkitspring.mybatis.internal.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import com.cuzz.bukkitspring.spi.platform.PluginResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MapperXmlLoader {
    private MapperXmlLoader() {
    }

    public static void registerMapper(SqlSessionFactory factory, String resourceId, InputStream input) {
        if (factory == null || resourceId == null || input == null) {
            return;
        }
        Configuration configuration = factory.getConfiguration();
        if (configuration.isResourceLoaded(resourceId)) {
            return;
        }
        XMLMapperBuilder builder = new XMLMapperBuilder(
                input,
                configuration,
                resourceId,
                configuration.getSqlFragments()
        );
        builder.parse();
    }

    public static int registerPluginMappers(PluginResource plugin, SqlSessionFactory factory, Logger logger) {
        if (plugin == null || factory == null) {
            return 0;
        }
        Path dataDir = plugin.getDataDirectory();
        Path sourcePath = plugin.getSourcePath();
        ClassLoader pluginLoader = plugin.getClassLoader();
        ClassLoader previous = Resources.getDefaultClassLoader();
        boolean switched = pluginLoader != null && pluginLoader != previous;
        if (switched) {
            Resources.setDefaultClassLoader(pluginLoader);
        }
        try {
            if (dataDir != null) {
                Path mapperDir = dataDir.resolve("mappers");
                if (sourcePath != null) {
                    extractMapperResources(plugin, sourcePath, mapperDir, logger);
                }
                return registerFromDirectory(plugin, mapperDir, factory, logger);
            }
            if (sourcePath == null) {
                return 0;
            }
            if (Files.isDirectory(sourcePath)) {
                return registerFromDirectory(plugin, sourcePath.resolve("mappers"), factory, logger);
            }
            if (!Files.exists(sourcePath)) {
                return 0;
            }
            return registerFromJar(plugin, sourcePath, factory, logger);
        } finally {
            if (switched) {
                Resources.setDefaultClassLoader(previous);
            }
        }
    }

    private static void extractMapperResources(PluginResource plugin, Path sourcePath, Path targetDir, Logger logger) {
        if (sourcePath == null || targetDir == null) {
            return;
        }
        try {
            Files.createDirectories(targetDir);
        } catch (IOException ex) {
            if (logger != null) {
                logger.log(Level.WARNING, "[MyBatis] Failed to create mapper dir for " + plugin.getName(), ex);
            }
            return;
        }
        if (Files.isDirectory(sourcePath)) {
            extractFromDirectory(plugin, sourcePath.resolve("mappers"), targetDir, logger);
            return;
        }
        if (!Files.exists(sourcePath)) {
            return;
        }
        extractFromJar(plugin, sourcePath, targetDir, logger);
    }

    private static void extractFromJar(PluginResource plugin, Path jarPath, Path targetDir, Logger logger) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!name.startsWith("mappers/") || !name.endsWith(".xml")) {
                    continue;
                }
                String relative = name.substring("mappers/".length());
                Path target = targetDir.resolve(relative);
                if (Files.exists(target)) {
                    continue;
                }
                try (InputStream input = jarFile.getInputStream(entry)) {
                    Files.createDirectories(target.getParent());
                    Files.copy(input, target);
                }
            }
        } catch (Exception ex) {
            if (logger != null) {
                logger.log(Level.WARNING, "[MyBatis] Failed to extract mapper XML for " + plugin.getName(), ex);
            }
        }
    }

    private static void extractFromDirectory(PluginResource plugin, Path sourceDir, Path targetDir, Logger logger) {
        if (sourceDir == null || !Files.exists(sourceDir)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(sourceDir)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                String name = path.getFileName().toString();
                if (!name.endsWith(".xml")) {
                    continue;
                }
                Path relative = sourceDir.relativize(path);
                Path target = targetDir.resolve(relative);
                if (Files.exists(target)) {
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.copy(path, target);
            }
        } catch (Exception ex) {
            if (logger != null) {
                logger.log(Level.WARNING, "[MyBatis] Failed to extract mapper XML for " + plugin.getName(), ex);
            }
        }
    }

    private static int registerFromJar(PluginResource plugin, Path jarPath, SqlSessionFactory factory, Logger logger) {
        int registered = 0;
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            Configuration configuration = factory.getConfiguration();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("mappers/") || !name.endsWith(".xml")) {
                    continue;
                }
                String resourceId = plugin.getName() + ":" + name;
                if (configuration.isResourceLoaded(resourceId)) {
                    continue;
                }
                try (InputStream input = jarFile.getInputStream(entry)) {
                    registerMapper(factory, resourceId, input);
                    registered++;
                }
            }
        } catch (Exception ex) {
            if (logger != null) {
                logger.log(Level.WARNING, "[MyBatis] Failed to register mapper XML for " + plugin.getName(), ex);
            }
        }
        return registered;
    }

    private static int registerFromDirectory(PluginResource plugin, Path mapperDir, SqlSessionFactory factory, Logger logger) {
        if (mapperDir == null || !Files.exists(mapperDir)) {
            return 0;
        }
        int registered = 0;
        Configuration configuration = factory.getConfiguration();
        try (java.util.stream.Stream<Path> stream = Files.walk(mapperDir)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                String name = mapperDir.relativize(path).toString().replace(File.separatorChar, '/');
                if (!name.endsWith(".xml")) {
                    continue;
                }
                String resourceId = plugin.getName() + ":mappers/" + name;
                if (configuration.isResourceLoaded(resourceId)) {
                    continue;
                }
                try (InputStream input = Files.newInputStream(path, StandardOpenOption.READ)) {
                    try {
                        registerMapper(factory, resourceId, input);
                        registered++;
                    } catch (Exception ex) {
                        if (logger != null) {
                            logger.log(Level.WARNING, "[MyBatis] Failed to register mapper XML for " + plugin.getName(), ex);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            if (logger != null) {
                logger.log(Level.WARNING, "[MyBatis] Failed to register mapper XML for " + plugin.getName(), ex);
            }
        }
        return registered;
    }
}
