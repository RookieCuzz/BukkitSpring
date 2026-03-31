package com.cuzz.starter.bukkitspring.mybatis.autoconfigure;

import com.cuzz.bukkitspring.dependency.MavenDependency;

import java.util.List;

public final class MybatisDependencies {
    private MybatisDependencies() {
    }

    public static List<MavenDependency> get() {
        return List.of(
                new MavenDependency("org.mybatis", "mybatis", "3.5.6"),
                new MavenDependency("com.zaxxer", "HikariCP", "4.0.3"),
                new MavenDependency("mysql", "mysql-connector-java", "8.0.23")
        );
    }
}
