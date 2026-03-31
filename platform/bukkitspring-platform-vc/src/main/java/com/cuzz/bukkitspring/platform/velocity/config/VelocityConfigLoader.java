package com.cuzz.bukkitspring.platform.velocity.config;

import com.cuzz.bukkitspring.spi.config.ConfigView;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class VelocityConfigLoader {
    private static final String DEFAULT_CONFIG = "config.yml";

    private final Path configPath;
    private final Logger logger;
    private final ClassLoader classLoader;

    public VelocityConfigLoader(Path configPath, Logger logger, ClassLoader classLoader) {
        this.configPath = Objects.requireNonNull(configPath, "configPath");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    public ConfigView load() {
        ensureDefaultConfig();
        Map<String, Object> data = loadYaml();
        return new VelocityConfigView(data);
    }

    private Map<String, Object> loadYaml() {
        if (!Files.exists(configPath)) {
            return Collections.emptyMap();
        }
        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            Object loaded = yaml.load(inputStream);
            if (loaded instanceof Map<?, ?> mapValue) {
                return VelocityConfigView.normalizeMap(mapValue);
            }
            return Collections.emptyMap();
        } catch (IOException ex) {
            logger.warning("Failed to read config: " + ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private void ensureDefaultConfig() {
        if (Files.exists(configPath)) {
            return;
        }
        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (InputStream inputStream = classLoader.getResourceAsStream(DEFAULT_CONFIG)) {
                if (inputStream != null) {
                    Files.copy(inputStream, configPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.createFile(configPath);
                }
            }
        } catch (IOException ex) {
            logger.warning("Failed to create default config: " + ex.getMessage());
        }
    }
}
