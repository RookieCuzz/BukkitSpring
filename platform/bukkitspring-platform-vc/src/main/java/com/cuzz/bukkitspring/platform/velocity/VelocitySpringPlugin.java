package com.cuzz.bukkitspring.platform.velocity;

import com.google.inject.Inject;
import com.cuzz.bukkitspring.BukkitSpring;
import com.cuzz.bukkitspring.api.ApplicationContext;
import com.cuzz.bukkitspring.api.StarterRegistry;
import com.cuzz.bukkitspring.dependency.BukkitSpringDependencies;
import com.cuzz.bukkitspring.dependency.DependencyDownloader;
import com.cuzz.bukkitspring.dependency.MavenDependency;
import com.cuzz.bukkitspring.dependency.VelocityDependencyAccess;
import com.cuzz.bukkitspring.platform.velocity.config.VelocityConfigLoader;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.bukkitspring.spi.starter.StarterContext;
import com.cuzz.bukkitspring.spi.starter.StarterLifecycle;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

@Plugin(id = "bukkitspring", name = "BukkitSpring", version = "1.0.0")
public final class VelocitySpringPlugin {
    private final ProxyServer server;
    private final org.slf4j.Logger slf4jLogger;
    private final Path dataDirectory;
    private final Logger julLogger;
    private ApplicationContext context;
    private boolean dependenciesReady = true;
    private final List<StarterLifecycle> loadedStarters = new ArrayList<>();

    @Inject
    public VelocitySpringPlugin(ProxyServer server,
                                org.slf4j.Logger slf4jLogger,
                                @DataDirectory Path dataDirectory) {
        this.server = Objects.requireNonNull(server, "server");
        this.slf4jLogger = Objects.requireNonNull(slf4jLogger, "slf4jLogger");
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.julLogger = createJulLogger(slf4jLogger);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        loadStarters();
        dependenciesReady = ensureDependencies();
        if (!dependenciesReady) {
            return;
        }
        ConfigView configView = loadConfig();
        initializeStarters(configView);

        List<Class<?>> configClasses = StarterRegistry.getAllConfigurations();
        List<String> scanPackages = StarterRegistry.getAllScanPackages();

        slf4jLogger.info("Found {} configuration classes from starters", configClasses.size());
        slf4jLogger.info("Found {} scan packages from starters", scanPackages.size());

        VelocityPlatformContext platformContext = new VelocityPlatformContext(
                this,
                server,
                slf4jLogger,
                julLogger,
                configView,
                dataDirectory
        );

        if (!scanPackages.isEmpty()) {
            context = BukkitSpring.registerPlugin(this, platformContext, scanPackages.toArray(new String[0]));
        } else {
            context = BukkitSpring.registerPlugin(this, platformContext);
        }
        context.refresh();

        slf4jLogger.info("BukkitSpring Velocity enabled. Waiting for plugins to register.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        BukkitSpring.shutdownAll();
        cleanupStarters();
        BukkitSpring.clearAllGlobalBeans();
    }


    private void initializeStarters(ConfigView configView) {
        if (loadedStarters.isEmpty()) {
            return;
        }
        StarterContext context = new VelocityStarterContext(julLogger, getClass().getClassLoader(), configView);
        for (StarterLifecycle starter : loadedStarters) {
            try {
                starter.initialize(context);
            } catch (Exception ex) {
                slf4jLogger.warn("Failed to initialize starter {}: {}", starter.getClass().getName(), ex.getMessage());
            }
        }
    }

    private void cleanupStarters() {
        if (loadedStarters.isEmpty()) {
            return;
        }
        for (StarterLifecycle starter : loadedStarters) {
            try {
                starter.cleanup();
            } catch (Exception ex) {
                slf4jLogger.warn("Failed to cleanup starter {}: {}", starter.getClass().getName(), ex.getMessage());
            }
        }
        loadedStarters.clear();
    }

    private void loadStarters() {
        File startersDir = resolveRootDir().resolve("starters").toFile();
        if (!startersDir.exists()) {
            startersDir.mkdirs();
            slf4jLogger.info("Created starters directory: {}", startersDir.getAbsolutePath());
            return;
        }

        File[] jarFiles = startersDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            slf4jLogger.info("No starter jars found in starters directory");
            return;
        }

        slf4jLogger.info("Found {} starter jar(s) in starters directory", jarFiles.length);

        DependencyDownloader downloader = createDownloader();
        for (File jarFile : jarFiles) {
            try {
                URL jarUrl = jarFile.toURI().toURL();
                downloader.addJarToClasspath(jarUrl);

                try (JarFile jar = new JarFile(jarFile)) {
                    Manifest manifest = jar.getManifest();
                    if (manifest != null) {
                        String starterClass = manifest.getMainAttributes().getValue("Starter-Class");
                        if (starterClass != null && !starterClass.isEmpty()) {
                            Class<?> starterType = Class.forName(starterClass, true, getClass().getClassLoader());
                            Object starterInstance = tryCreateStarter(starterType);
                            if (starterInstance instanceof StarterLifecycle lifecycle) {
                                loadedStarters.add(lifecycle);
                            }
                            slf4jLogger.info("Loaded starter from {}: {}", jarFile.getName(), starterClass);
                        } else {
                            slf4jLogger.warn("Starter jar {} does not define Starter-Class in MANIFEST.MF", jarFile.getName());
                        }
                    }
                }
            } catch (Exception ex) {
                slf4jLogger.warn("Failed to load starter from {}: {}", jarFile.getName(), ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private Object tryCreateStarter(Class<?> starterType) {
        try {
            return starterType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            slf4jLogger.warn("Failed to instantiate starter {}: {}", starterType.getName(), ex.getMessage());
            return null;
        }
    }

    private boolean ensureDependencies() {
        DependencyDownloader downloader = createDownloader();
        try {
            registerPlatformDependencies();
            StarterRegistry.registerDependencies(BukkitSpringDependencies.required());
            List<MavenDependency> allDependencies = StarterRegistry.getAllDependencies();

            slf4jLogger.info("Total dependencies to download: {}", allDependencies.size());
            downloader.ensureDependencies(allDependencies);
            return true;
        } catch (Exception ex) {
            slf4jLogger.error("Dependency download failed. Disabling plugin.", ex);
            return false;
        }
    }

    private void registerPlatformDependencies() {
        StarterRegistry.registerDependencies(List.of(
                new MavenDependency("org.slf4j", "slf4j-api", "1.7.36"),
                new MavenDependency("org.yaml", "snakeyaml", "2.2")
        ));
    }

    private ConfigView loadConfig() {
        Path configPath = resolveRootDir().resolve("config.yml");
        VelocityConfigLoader loader = new VelocityConfigLoader(configPath, julLogger, getClass().getClassLoader());
        return loader.load();
    }


    private DependencyDownloader createDownloader() {
        Path librariesDir = resolveServerRootDir().resolve("libraries");
        return new DependencyDownloader(new VelocityDependencyAccess(librariesDir, julLogger, getClass().getClassLoader()));
    }

    private Path resolveRootDir() {
        Path dataDir = dataDirectory.toAbsolutePath().normalize();
        Path parent = dataDir.getParent();
        if (parent == null) {
            return dataDirectory;
        }
        return parent.resolve("BukkitSpring");
    }

    private Path resolveServerRootDir() {
        Path dataDir = dataDirectory.toAbsolutePath().normalize();
        Path pluginsDir = dataDir.getParent();
        if (pluginsDir == null) {
            return dataDir;
        }
        Path serverRoot = pluginsDir.getParent();
        if (serverRoot == null) {
            return pluginsDir;
        }
        return serverRoot;
    }

    private static Logger createJulLogger(org.slf4j.Logger slf4jLogger) {
        Logger logger = Logger.getLogger("BukkitSpring");
        logger.setUseParentHandlers(false);
        for (Handler handler : logger.getHandlers()) {
            if (handler instanceof Slf4jHandler) {
                return logger;
            }
        }
        logger.addHandler(new Slf4jHandler(slf4jLogger));
        logger.setLevel(Level.ALL);
        return logger;
    }

    private static final class VelocityStarterContext implements StarterContext {
        private final Logger logger;
        private final ClassLoader classLoader;
        private final ConfigView config;

        private VelocityStarterContext(Logger logger, ClassLoader classLoader, ConfigView config) {
            this.logger = logger;
            this.classLoader = classLoader;
            this.config = config;
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }

        @Override
        public ConfigView getConfig() {
            return config;
        }

        @Override
        public <T> void registerGlobalBean(Class<T> type, T instance) {
            BukkitSpring.registerGlobalBean(type, instance);
        }
    }

    private static final class Slf4jHandler extends Handler {
        private final org.slf4j.Logger logger;

        private Slf4jHandler(org.slf4j.Logger logger) {
            this.logger = Objects.requireNonNull(logger, "logger");
        }

        @Override
        public void publish(LogRecord record) {
            if (record == null || !isLoggable(record)) {
                return;
            }
            Throwable thrown = record.getThrown();
            String message = record.getMessage();
            int level = record.getLevel().intValue();
            if (level >= Level.SEVERE.intValue()) {
                if (thrown != null) {
                    logger.error(message, thrown);
                } else {
                    logger.error(message);
                }
            } else if (level >= Level.WARNING.intValue()) {
                if (thrown != null) {
                    logger.warn(message, thrown);
                } else {
                    logger.warn(message);
                }
            } else if (level >= Level.INFO.intValue()) {
                if (thrown != null) {
                    logger.info(message, thrown);
                } else {
                    logger.info(message);
                }
            } else if (level >= Level.FINE.intValue()) {
                if (thrown != null) {
                    logger.debug(message, thrown);
                } else {
                    logger.debug(message);
                }
            } else {
                if (thrown != null) {
                    logger.trace(message, thrown);
                } else {
                    logger.trace(message);
                }
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
