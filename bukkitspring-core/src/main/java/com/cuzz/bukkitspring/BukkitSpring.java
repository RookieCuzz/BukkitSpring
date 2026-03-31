package com.cuzz.bukkitspring;

import com.cuzz.bukkitspring.api.ApplicationContext;
import com.cuzz.bukkitspring.internal.SimpleApplicationContext;
import com.cuzz.bukkitspring.spi.platform.PlatformContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BukkitSpring {
    private static final Map<Object, ApplicationContext> CONTEXTS = new ConcurrentHashMap<>();
    // 全局 Bean 存储：Class -> Instance
    private static final Map<Class<?>, Object> GLOBAL_BEANS = new ConcurrentHashMap<>();

    private BukkitSpring() {
    }

    public static synchronized ApplicationContext registerPlugin(Object key, PlatformContext platformContext, String... basePackages) {
        if (key == null || platformContext == null) {
            throw new IllegalArgumentException("Key and platformContext cannot be null");
        }
        ApplicationContext existing = CONTEXTS.get(key);
        if (existing != null) {
            return existing;
        }
        SimpleApplicationContext context = new SimpleApplicationContext(platformContext);
        if (basePackages != null && basePackages.length > 0) {
            context.scan(basePackages);
        }
        CONTEXTS.put(key, context);
        return context;
    }

    public static ApplicationContext getContext(Object key) {
        return CONTEXTS.get(key);
    }

    /**
     * 注册全局 Bean，供所有插件使用
     * 
     * @param type Bean 类型
     * @param instance Bean 实例
     */
    public static <T> void registerGlobalBean(Class<T> type, T instance) {
        if (type == null || instance == null) {
            throw new IllegalArgumentException("Type and instance cannot be null");
        }
        GLOBAL_BEANS.put(type, instance);
    }

    /**
     * 获取全局 Bean
     * 
     * @param type Bean 类型
     * @return Bean 实例，如果不存在则返回 null
     */
    public static <T> T getGlobalBean(Class<T> type) {
        return type.cast(GLOBAL_BEANS.get(type));
    }

    /**
     * 获取所有全局 Bean
     * 
     * @return 不可修改的全局 Bean Map
     */
    public static Map<Class<?>, Object> getAllGlobalBeans() {
        return Map.copyOf(GLOBAL_BEANS);
    }

    /**
     * 清除全局 Bean
     * 
     * @param type Bean 类型
     */
    public static void clearGlobalBean(Class<?> type) {
        GLOBAL_BEANS.remove(type);
    }

    /**
     * 清除所有全局 Bean
     */
    public static void clearAllGlobalBeans() {
        GLOBAL_BEANS.clear();
    }

    public static void unregisterPlugin(Object key) {
        ApplicationContext context = CONTEXTS.remove(key);
        if (context != null) {
            context.close();
        }
    }

    public static void shutdownAll() {
        for (ApplicationContext context : CONTEXTS.values()) {
            context.close();
        }
        CONTEXTS.clear();
    }

}
