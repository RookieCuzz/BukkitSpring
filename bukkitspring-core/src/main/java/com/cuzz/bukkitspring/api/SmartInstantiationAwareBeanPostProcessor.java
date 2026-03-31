package com.cuzz.bukkitspring.api;

public interface SmartInstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    default Object getEarlyBeanReference(Object bean, String name) {
        return bean;
    }
}
