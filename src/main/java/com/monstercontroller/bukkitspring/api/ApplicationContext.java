package com.monstercontroller.bukkitspring.api;

public interface ApplicationContext {
    <T> T get(Class<T> type);

    <T> T get(Class<T> type, String qualifier);

    <T> void bindInstance(Class<T> type, T instance);

    <T> void bindProvider(Class<T> type, Provider<T> provider);

    void scan(String... basePackages);

    void refresh();

    void close();
}
