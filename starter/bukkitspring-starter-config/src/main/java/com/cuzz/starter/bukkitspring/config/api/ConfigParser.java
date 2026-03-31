package com.cuzz.starter.bukkitspring.config.api;

/**
 * Plugin-side parser rule that converts loaded text into typed config objects.
 */
@FunctionalInterface
public interface ConfigParser<T> {
    T parse(ConfigDocument document) throws Exception;
}
