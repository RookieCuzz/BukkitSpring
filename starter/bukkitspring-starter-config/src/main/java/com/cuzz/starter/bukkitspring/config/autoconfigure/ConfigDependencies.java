package com.cuzz.starter.bukkitspring.config.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;

import java.util.Collections;
import java.util.List;

/**
 * Config starter dependency definitions.
 */
public final class ConfigDependencies {

    private ConfigDependencies() {
    }

    public static List<MavenDependency> get() {
        return Collections.emptyList();
    }
}
