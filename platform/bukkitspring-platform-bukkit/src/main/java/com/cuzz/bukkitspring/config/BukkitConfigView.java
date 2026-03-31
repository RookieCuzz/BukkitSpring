package com.cuzz.bukkitspring.config;

import com.cuzz.bukkitspring.spi.config.ConfigSection;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;
import java.util.Set;

public final class BukkitConfigView implements ConfigView {
    private final FileConfiguration config;

    public BukkitConfigView(FileConfiguration config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    @Override
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }

    @Override
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    @Override
    public long getLong(String path, long defaultValue) {
        return config.getLong(path, defaultValue);
    }

    @Override
    public ConfigSection getSection(String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return null;
        }
        return new BukkitConfigSection(section);
    }

    private static final class BukkitConfigSection implements ConfigSection {
        private final ConfigurationSection section;

        private BukkitConfigSection(ConfigurationSection section) {
            this.section = section;
        }

        @Override
        public Set<String> keys() {
            return section.getKeys(false);
        }

        @Override
        public Object get(String key) {
            return section.get(key);
        }
    }
}
