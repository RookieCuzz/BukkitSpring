package com.cuzz.bukkitspring.api;

import com.cuzz.bukkitspring.dependency.MavenDependency;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Starter 注册中心
 * 
 * <p>提供统一的 API 让 Starter 注册自己的依赖和配置类。
 * Starter 应该在静态初始化块中调用此类的方法进行注册。
 * 
 * <p>注册时机：
 * <ul>
 *   <li>依赖注册：在 BukkitSpringPlugin.onLoad() 之前（静态初始化块）</li>
 *   <li>配置类注册：在 BukkitSpringPlugin.onLoad() 之前（静态初始化块）</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>{@code
 * public class KafkaStarter {
 *     static {
 *         // 注册依赖
 *         StarterRegistry.registerDependencies(Arrays.asList(
 *             new MavenDependency("org.apache.kafka", "kafka-clients", "3.7.0")
 *         ));
 *         
 *         // 注册配置类
 *         StarterRegistry.registerConfiguration(KafkaAutoConfiguration.class);
 *     }
 * }
 * }</pre>
 */
public final class StarterRegistry {
    
    private static final Set<MavenDependency> dependencies = new LinkedHashSet<>();
    private static final Set<Class<?>> configurationClasses = new LinkedHashSet<>();
    private static final Set<String> scanPackages = new LinkedHashSet<>();
    
    private StarterRegistry() {
    }
    
    /**
     * 注册单个依赖
     * 
     * @param dependency Maven 依赖
     */
    public static synchronized void registerDependency(MavenDependency dependency) {
        if (dependency != null) {
            dependencies.add(dependency);
        }
    }
    
    /**
     * 批量注册依赖
     * 
     * @param deps Maven 依赖列表
     */
    public static synchronized void registerDependencies(List<MavenDependency> deps) {
        if (deps != null) {
            dependencies.addAll(deps);
        }
    }
    
    /**
     * 注册配置类
     * 
     * <p>配置类需要使用 @Configuration 注解标记。
     * 
     * @param configClass 配置类
     */
    public static synchronized void registerConfiguration(Class<?> configClass) {
        if (configClass != null) {
            configurationClasses.add(configClass);
        }
    }
    
    /**
     * 批量注册配置类
     * 
     * @param configClasses 配置类列表
     */
    public static synchronized void registerConfigurations(List<Class<?>> configClasses) {
        if (configClasses != null) {
            configurationClasses.addAll(configClasses);
        }
    }
    
    /**
     * 注册扫描包
     * 
     * <p>框架会扫描该包下所有带 @Configuration 注解的类。
     * 
     * @param packageName 包名
     */
    public static synchronized void registerScanPackage(String packageName) {
        if (packageName != null && !packageName.isBlank()) {
            scanPackages.add(packageName);
        }
    }
    
    /**
     * 获取所有已注册的依赖
     * 
     * @return 依赖列表
     */
    public static synchronized List<MavenDependency> getAllDependencies() {
        return new ArrayList<>(dependencies);
    }
    
    /**
     * 获取所有已注册的配置类
     * 
     * @return 配置类列表
     */
    public static synchronized List<Class<?>> getAllConfigurations() {
        return new ArrayList<>(configurationClasses);
    }
    
    /**
     * 获取所有已注册的扫描包
     * 
     * @return 包名列表
     */
    public static synchronized List<String> getAllScanPackages() {
        return new ArrayList<>(scanPackages);
    }
    
    /**
     * 清空所有注册信息（仅供测试使用）
     */
    public static synchronized void clear() {
        dependencies.clear();
        configurationClasses.clear();
        scanPackages.clear();
    }
}
