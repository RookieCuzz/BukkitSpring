package com.cuzz.bukkitspring.spi.config;

import java.util.Set;

public interface ConfigSection {
    Set<String> keys();

    Object get(String key);
}
