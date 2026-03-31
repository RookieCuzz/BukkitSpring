package com.cuzz.bukkitspring.platform.velocity.resource;

import com.cuzz.bukkitspring.spi.platform.PluginResource;
import com.cuzz.bukkitspring.spi.platform.PluginResourceResolver;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class VelocityPluginResourceResolver implements PluginResourceResolver {
    private final ProxyServer server;

    public VelocityPluginResourceResolver(ProxyServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public List<PluginResource> listPlugins() {
        List<PluginResource> resources = new ArrayList<>();
        for (PluginContainer container : server.getPluginManager().getPlugins()) {
            resources.add(new VelocityPluginResource(container));
        }
        return resources;
    }

    private static final class VelocityPluginResource implements PluginResource {
        private final PluginContainer container;
        private final Object pluginKey;
        private final ClassLoader classLoader;
        private final String mainPackage;
        private final Path sourcePath;
        private final Path dataDirectory;

        private VelocityPluginResource(PluginContainer container) {
            this.container = container;
            Optional<?> instance = container.getInstance();
            Object instanceValue = instance.orElse(null);
            this.pluginKey = instanceValue != null ? instanceValue : container;
            this.classLoader = instanceValue != null ? instanceValue.getClass().getClassLoader() : null;
            this.mainPackage = instanceValue != null ? instanceValue.getClass().getPackageName() : "";
            this.sourcePath = resolveSourcePath(instanceValue);
            this.dataDirectory = resolveDataDirectory(instanceValue, container, sourcePath);
        }

        @Override
        public Object getKey() {
            return pluginKey;
        }

        @Override
        public String getName() {
            return container.getDescription().getId();
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public String getMainPackage() {
            return mainPackage;
        }

        @Override
        public Path getSourcePath() {
            return sourcePath;
        }

        @Override
        public InputStream openResource(String path) {
            if (classLoader == null) {
                return null;
            }
            return classLoader.getResourceAsStream(path);
        }

        @Override
        public Path getDataDirectory() {
            return dataDirectory;
        }
    }

    private static Path resolveSourcePath(Object instance) {
        if (instance == null) {
            return null;
        }
        try {
            URL url = instance.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (url == null) {
                return null;
            }
            return Paths.get(url.toURI());
        } catch (Exception ex) {
            return null;
        }
    }

    private static Path resolveDataDirectory(Object instance, PluginContainer container, Path sourcePath) {
        if (instance != null) {
            Path direct = resolveDataDirectoryFromInstance(instance);
            if (direct != null) {
                return direct;
            }
        }
        if (sourcePath == null) {
            return null;
        }
        String id = container.getDescription().getId();
        if (id == null || id.isBlank()) {
            return null;
        }
        Path parent = sourcePath.getParent();
        if (parent == null) {
            return null;
        }
        return parent.resolve(id);
    }

    private static Path resolveDataDirectoryFromInstance(Object instance) {
        try {
            Method method = instance.getClass().getMethod("getDataDirectory");
            Object value = method.invoke(instance);
            if (value instanceof Path path) {
                return path;
            }
            if (value instanceof File file) {
                return file.toPath();
            }
        } catch (Exception ignored) {
        }
        try {
            Method method = instance.getClass().getMethod("getDataFolder");
            Object value = method.invoke(instance);
            if (value instanceof Path path) {
                return path;
            }
            if (value instanceof File file) {
                return file.toPath();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
