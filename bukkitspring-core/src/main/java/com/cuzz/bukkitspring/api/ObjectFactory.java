package com.cuzz.bukkitspring.api;

@FunctionalInterface
public interface ObjectFactory<T> {
    T getObject();
}
