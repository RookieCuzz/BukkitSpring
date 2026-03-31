package com.cuzz.starter.bukkitspring.mybatis.internal;

import com.cuzz.bukkitspring.BukkitSpring;
import com.cuzz.bukkitspring.api.ApplicationContext;
import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.starter.bukkitspring.mybatis.core.MybatisService;
import com.cuzz.starter.bukkitspring.mybatis.internal.mapper.MapperXmlLoader;
import com.cuzz.starter.bukkitspring.mybatis.internal.mapper.MybatisMapperProxy;
import com.cuzz.bukkitspring.spi.platform.PluginResource;
import com.cuzz.bukkitspring.spi.platform.PluginResourceResolver;
import com.cuzz.bukkitspring.spi.platform.PlatformScheduler;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Component
public final class MybatisMapperInitializer {
    private final MybatisService service;
    private final PluginResourceResolver pluginResourceResolver;
    private final Logger logger;
    private final PlatformScheduler scheduler;
    private final Set<Class<?>> registeredMappers = ConcurrentHashMap.newKeySet();

    @Autowired
    public MybatisMapperInitializer(MybatisService service,
                                    PluginResourceResolver pluginResourceResolver,
                                    Logger logger,
                                    @Autowired(required = false) PlatformScheduler scheduler) {
        this.service = service;
        this.pluginResourceResolver = pluginResourceResolver;
        this.logger = logger;
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void initialize() {
        scanAll();
        scheduleRescan();
    }

    private void scheduleRescan() {
        if (scheduler == null) {
            return;
        }
        scheduler.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
            scanAll();
        });
    }

    private void scanAll() {
        if (!service.isEnabled()) {
            logger.info("[MyBatis] Mapper scan skipped (disabled).");
            return;
        }
        if (pluginResourceResolver == null) {
            logger.warning("[MyBatis] PluginResourceResolver not available, skipping mapper scan.");
            return;
        }
        int totalPlugins = 0;
        int totalMappers = 0;
        for (PluginResource plugin : pluginResourceResolver.listPlugins()) {
            PluginScanResult result = scanPlugin(plugin);
            if (result == null) {
                continue;
            }
            totalPlugins++;
            totalMappers += result.registered;
            logger.info("[MyBatis] " + plugin.getName()
                    + " mappers=" + result.registered
                    + " xml=" + result.xmlCount);
        }
        logger.info("[MyBatis] Mapper scan completed. plugins=" + totalPlugins + ", mappers=" + totalMappers);
    }

    private PluginScanResult scanPlugin(PluginResource plugin) {
        ApplicationContext context = BukkitSpring.getContext(plugin.getKey());
        if (context == null) {
            return null;
        }
        List<String> packages = resolveScanPackages(context, plugin);
        if (packages.isEmpty()) {
            return null;
        }
        Set<Class<?>> mapperTypes = scanMapperInterfaces(plugin, packages);
        int xmlCount = MapperXmlLoader.registerPluginMappers(plugin, service.getSqlSessionFactory(), logger);
        if (mapperTypes.isEmpty() && xmlCount == 0) {
            return null;
        }
        int registered = registerMapperTypes(context, mapperTypes);
        return new PluginScanResult(registered, xmlCount);
    }

    private int registerMapperTypes(ApplicationContext context, Set<Class<?>> mapperTypes) {
        int registered = 0;
        for (Class<?> mapperType : mapperTypes) {
            if (!registeredMappers.add(mapperType)) {
                registerMapperType(mapperType);
                continue;
            }
            if (registerMapperBean(context, mapperType)) {
                registered++;
            } else {
                registeredMappers.remove(mapperType);
            }
            registerMapperType(mapperType);
        }
        return registered;
    }

    private static final class PluginScanResult {
        private final int registered;
        private final int xmlCount;

        private PluginScanResult(int registered, int xmlCount) {
            this.registered = registered;
            this.xmlCount = xmlCount;
        }
    }

    private boolean registerMapperBean(ApplicationContext context, Class<?> mapperType) {
        try {
            @SuppressWarnings("unchecked")
            Class<Object> type = (Class<Object>) mapperType;
            context.bindProvider(type, () -> MybatisMapperProxy.create(type, service));
            return true;
        } catch (RuntimeException ex) {
            logger.warning("[MyBatis] Failed to bind mapper " + mapperType.getName() + ": " + ex.getMessage());
            return false;
        }
    }

    private void registerMapperType(Class<?> mapperType) {
        Configuration configuration = service.getSqlSessionFactory().getConfiguration();
        if (configuration.hasMapper(mapperType)) {
            return;
        }
        try {
            configuration.addMapper(mapperType);
        } catch (RuntimeException ex) {
            logger.warning("[MyBatis] Failed to add mapper to configuration: " + mapperType.getName());
        }
    }

    private Set<Class<?>> scanMapperInterfaces(PluginResource plugin, List<String> packages) {
        Set<Class<?>> mappers = new LinkedHashSet<>();
        String[] packageArray = packages.toArray(new String[0]);
        ClassLoader classLoader = plugin.getClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        try (ScanResult result = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .overrideClassLoaders(classLoader)
                .acceptPackages(packageArray)
                .scan()) {
            for (ClassInfo info : result.getClassesWithAnnotation(Mapper.class.getName())) {
                Class<?> type = info.loadClass();
                if (type.isInterface()) {
                    mappers.add(type);
                }
            }
        } catch (Exception ex) {
            logger.warning("[MyBatis] Mapper scan failed for " + plugin.getName() + ": " + ex.getMessage());
        }
        return mappers;
    }

    private List<String> resolveScanPackages(ApplicationContext context, PluginResource plugin) {
        List<String> packages = new ArrayList<>();
        try {
            Method method = context.getClass().getMethod("getScannedPackages");
            Object value = method.invoke(context);
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String name) {
                        String trimmed = name.trim();
                        if (!trimmed.isEmpty()) {
                            packages.add(trimmed);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (packages.isEmpty()) {
            String fallback = plugin.getMainPackage();
            if (fallback != null && !fallback.isBlank()) {
                packages.add(fallback);
            }
        }
        return packages;
    }
}


