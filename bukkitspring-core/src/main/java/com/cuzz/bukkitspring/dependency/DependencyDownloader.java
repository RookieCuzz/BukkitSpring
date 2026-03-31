package com.cuzz.bukkitspring.dependency;

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
import java.util.zip.ZipFile;

public final class DependencyDownloader {
    private static final String[] MAVEN_MIRRORS = {
            "https://repo.maven.apache.org/maven2/",
            "https://repo1.maven.org/maven2/",
            "https://repo.huaweicloud.com/repository/maven/",
            "https://maven.aliyun.com/repository/central/"
    };

    private final DependencyAccess access;
    private final Logger logger;
    private final Path librariesDir;

    public DependencyDownloader(DependencyAccess access) {
        this.access = Objects.requireNonNull(access, "access");
        this.logger = Objects.requireNonNull(access.getLogger(), "logger");
        this.librariesDir = Objects.requireNonNull(access.getLibrariesDirectory(), "librariesDir");
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
            if (isValidJar(jarPath)) {
                return;
            }
            logger.warning("Detected corrupted dependency jar, re-downloading: " + jarPath.getFileName());
            Files.deleteIfExists(jarPath);
        }
        Files.createDirectories(jarPath.getParent());
        logger.info("Downloading dependency: " + dependency);
        IOException lastError = null;
        for (String mirror : MAVEN_MIRRORS) {
            String url = mirror + dependency.relativePath();
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, jarPath, StandardCopyOption.REPLACE_EXISTING);
                if (!isValidJar(jarPath)) {
                    Files.deleteIfExists(jarPath);
                    throw new IOException("Downloaded jar is corrupted: " + dependency);
                }
                return;
            } catch (IOException ex) {
                lastError = ex;
                logger.warning("Failed to download from " + mirror + ": " + ex.getMessage());
            }
        }
        if (lastError != null) {
            throw lastError;
        }
    }

    private boolean isValidJar(Path jarPath) {
        try (ZipFile ignored = new ZipFile(jarPath.toFile())) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void addToClasspath(Path jarPath) throws ReflectiveOperationException, IOException {
        URL url = jarPath.toUri().toURL();
        ClassLoader primary = access.getPrimaryClassLoader();
        addToClassLoader(primary, url, true);
        ClassLoader secondary = access.getSecondaryClassLoader();
        if (secondary != null && secondary != primary) {
            try {
                addToClassLoader(secondary, url, false);
            } catch (ReflectiveOperationException | RuntimeException ex) {
                logger.warning("Failed to expose dependency to secondary classloader: " + ex.getMessage());
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

    public void addJarToClasspath(URL jarUrl) throws ReflectiveOperationException {
        ClassLoader primary = access.getPrimaryClassLoader();
        addToClassLoader(primary, jarUrl, true);
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
