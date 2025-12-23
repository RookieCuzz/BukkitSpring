package com.monstercontroller.bukkitspring.internal;

import com.monstercontroller.bukkitspring.api.annotation.ScopeType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class BeanDefinition {
    private final String name;
    private final Class<?> type;
    private final ScopeType scope;
    private final boolean primary;
    private final Constructor<?> constructor;
    private final List<InjectionPoint> injectionPoints = new ArrayList<>();
    private final Method postConstruct;
    private final Method preDestroy;
    private final Method factoryMethod;
    private final boolean factoryMethodStatic;
    private final String factoryBeanName;
    private final Supplier<?> instanceSupplier;

    BeanDefinition(
            String name,
            Class<?> type,
            ScopeType scope,
            boolean primary,
            Constructor<?> constructor,
            Method postConstruct,
            Method preDestroy,
            Method factoryMethod,
            boolean factoryMethodStatic,
            String factoryBeanName,
            Supplier<?> instanceSupplier
    ) {
        this.name = name;
        this.type = type;
        this.scope = scope;
        this.primary = primary;
        this.constructor = constructor;
        this.postConstruct = postConstruct;
        this.preDestroy = preDestroy;
        this.factoryMethod = factoryMethod;
        this.factoryMethodStatic = factoryMethodStatic;
        this.factoryBeanName = factoryBeanName;
        this.instanceSupplier = instanceSupplier;
    }

    String getName() {
        return name;
    }

    Class<?> getType() {
        return type;
    }

    ScopeType getScope() {
        return scope;
    }

    boolean isPrimary() {
        return primary;
    }

    Constructor<?> getConstructor() {
        return constructor;
    }

    List<InjectionPoint> getInjectionPoints() {
        return injectionPoints;
    }

    Method getPostConstruct() {
        return postConstruct;
    }

    Method getPreDestroy() {
        return preDestroy;
    }

    Method getFactoryMethod() {
        return factoryMethod;
    }

    boolean isFactoryMethodStatic() {
        return factoryMethodStatic;
    }

    String getFactoryBeanName() {
        return factoryBeanName;
    }

    Supplier<?> getInstanceSupplier() {
        return instanceSupplier;
    }

    boolean isPrototype() {
        return scope == ScopeType.PROTOTYPE;
    }
}
