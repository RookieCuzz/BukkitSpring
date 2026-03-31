package com.cuzz.bukkitspring.spi.config;

public interface ConfigView {
    boolean getBoolean(String path, boolean defaultValue);

    String getString(String path, String defaultValue);

    int getInt(String path, int defaultValue);

    long getLong(String path, long defaultValue);

    ConfigSection getSection(String path);
}
