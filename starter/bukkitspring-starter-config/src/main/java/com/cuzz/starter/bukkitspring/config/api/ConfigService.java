package com.cuzz.starter.bukkitspring.config.api;

/**
 * Unified config loading service with local/remote strategies.
 */
public interface ConfigService extends AutoCloseable {
    boolean isEnabled();

    ConfigDocument load(String name);

    ConfigDocument load(ConfigQuery query);

    <T> T load(ConfigQuery query, ConfigParser<T> parser);

    default <T> T load(String name, ConfigParser<T> parser) {
        return load(ConfigQuery.of(name), parser);
    }

    void invalidate(String name);

    void invalidateAll();

    @Override
    void close();
}
