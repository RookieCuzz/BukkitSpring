package com.monstercontroller.bukkitspring.dependency;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class DependencyDownloader {
    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Path librariesDir;

    public DependencyDownloader(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = plugin.getLogger();
        this.librariesDir = plugin.getServer().getWorldContainer().toPath().resolve("libraries");
    }

    public void ensureDependencies(List<MavenDependency> dependencies) throws IOException, ReflectiveOperationException {
        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }
        Files.createDirectories(librariesDir);
        for (MavenDependency dependency : dependencies) {
            Path jarPath = librariesDir.resolve(dependency.relativePath());
            downloadIfMissing(dependency, jarPath);
            addToClasspath(jarPath);
        }
    }

    private void downloadIfMissing(MavenDependency dependency, Path jarPath) throws IOException {
        if (Files.exists(jarPath)) {
            return;
        }
        Files.createDirectories(jarPath.getParent());
        String url = MAVEN_CENTRAL + dependency.relativePath();
        logger.info("Downloading dependency: " + dependency);
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, jarPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void addToClasspath(Path jarPath) throws ReflectiveOperationException, IOException {
        URL url = jarPath.toUri().toURL();
        ClassLoader pluginLoader = plugin.getClass().getClassLoader();
        addToClassLoader(pluginLoader, url, true);
        ClassLoader serverLoader = plugin.getServer().getClass().getClassLoader();
        if (serverLoader != null && serverLoader != pluginLoader) {
            try {
                addToClassLoader(serverLoader, url, false);
            } catch (ReflectiveOperationException | RuntimeException ex) {
                logger.warning("Failed to expose dependency to server classloader: " + ex.getMessage());
            }
        }
    }

    private static boolean hasUrl(URLClassLoader classLoader, URL url) {
        for (URL existing : classLoader.getURLs()) {
            if (existing.equals(url)) {
                return true;
            }
        }
        return false;
    }

    private static Method findAddUrl(ClassLoader loader) {
        Class<?> current = loader.getClass();
        while (current != null && current != URLClassLoader.class) {
            try {
                return current.getDeclaredMethod("addURL", URL.class);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private void addToClassLoader(ClassLoader classLoader, URL url, boolean required)
            throws ReflectiveOperationException {
        if (classLoader == null) {
            if (required) {
                throw new IllegalStateException("ClassLoader is null.");
            }
            return;
        }
        if (classLoader instanceof URLClassLoader urlClassLoader && hasUrl(urlClassLoader, url)) {
            return;
        }
        Method addUrl = findAddUrl(classLoader);
        if (addUrl != null) {
            try {
                invokeAddUrl(classLoader, addUrl, url);
                return;
            } catch (InaccessibleObjectException ex) {
                if (!(classLoader instanceof URLClassLoader)) {
                    if (required) {
                        throw new IllegalStateException("ClassLoader addURL is not accessible: " +
                                classLoader.getClass().getName(), ex);
                    }
                    return;
                }
            }
        }
        if (classLoader instanceof URLClassLoader urlClassLoader) {
            if (tryInvokeBaseAddUrl(urlClassLoader, url)) {
                return;
            }
            URLClassLoaderAccess access = URLClassLoaderAccess.create(urlClassLoader);
            try {
                access.addURL(url);
                return;
            } catch (UnsupportedOperationException ex) {
                if (required) {
                    throw new IllegalStateException("Cannot add URL to classloader: " +
                            classLoader.getClass().getName(), ex);
                }
                return;
            }
        }
        if (required) {
            throw new IllegalStateException("Unsupported class loader: " + classLoader.getClass().getName());
        }
    }

    private static boolean tryInvokeBaseAddUrl(URLClassLoader loader, URL url) throws ReflectiveOperationException {
        try {
            Method baseAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            if (!baseAddUrl.canAccess(loader)) {
                baseAddUrl.setAccessible(true);
            }
            baseAddUrl.invoke(loader, url);
            return true;
        } catch (InaccessibleObjectException ex) {
            return false;
        }
    }

    private void invokeAddUrl(ClassLoader loader, Method addUrl, URL url) throws ReflectiveOperationException {
        if (!addUrl.canAccess(loader)) {
            addUrl.setAccessible(true);
        }
        addUrl.invoke(loader, url);
    }
}
