package com.cuzz.starter.bukkitspring.mybatis.internal;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.bukkitspring.api.annotation.PreDestroy;
import com.cuzz.starter.bukkitspring.mybatis.config.MybatisSettings;
import com.cuzz.starter.bukkitspring.mybatis.core.MybatisService;
import com.cuzz.starter.bukkitspring.mybatis.internal.MybatisSessionFactoryBuilder;
import com.cuzz.starter.bukkitspring.mybatis.internal.mapper.MapperResourceResolver;
import com.cuzz.starter.bukkitspring.mybatis.internal.mapper.MapperXmlLoader;
import com.cuzz.bukkitspring.spi.platform.PluginResource;
import com.cuzz.bukkitspring.spi.platform.PluginResourceResolver;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public final class DefaultMybatisService implements MybatisService, AutoCloseable {
    private final MybatisSettings settings;
    private final PluginResourceResolver pluginResourceResolver;
    private final Logger logger;
    private final MybatisSessionFactoryBuilder sessionFactoryBuilder;
    private final MapperResourceResolver mapperResourceResolver;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile SqlSessionFactory sqlSessionFactory;
    private volatile boolean enabled;

    @Autowired
    public DefaultMybatisService(MybatisSettings settings, PluginResourceResolver pluginResourceResolver, Logger logger) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.pluginResourceResolver = Objects.requireNonNull(pluginResourceResolver, "pluginResourceResolver");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.sessionFactoryBuilder = new MybatisSessionFactoryBuilder();
        this.mapperResourceResolver = new MapperResourceResolver();
    }

    @PostConstruct
    public void initialize() {
        if (!settings.enabled) {
            return;
        }
        if (!settings.hasRequiredJdbc()) {
            logger.warning("[MyBatis] Missing jdbc-url. MyBatis disabled.");
            return;
        }
        try {
            sqlSessionFactory = sessionFactoryBuilder.build(settings, logger);
            enabled = true;
            registerGlobalBeans();
            logger.info("[MyBatis] SqlSessionFactory ready.");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "[MyBatis] Failed to initialize.", ex);
            enabled = false;
            sqlSessionFactory = null;
        }
    }

    @PreDestroy
    public void preDestroy() {
        close();
    }

    @Override
    public boolean isEnabled() {
        return enabled && !closed.get();
    }

    @Override
    public boolean isAutoCommit() {
        return settings.autoCommit;
    }

    @Override
    public SqlSessionFactory getSqlSessionFactory() {
        ensureEnabled();
        return sqlSessionFactory;
    }

    @Override
    public SqlSession openSession(boolean autoCommit) {
        ensureEnabled();
        return sqlSessionFactory.openSession(autoCommit);
    }

    @Override
    public <T> T withSession(Function<SqlSession, T> work) {
        ensureEnabled();
        try (SqlSession session = sqlSessionFactory.openSession(settings.autoCommit)) {
            return work.apply(session);
        }
    }

    @Override
    public void withSession(Consumer<SqlSession> work) {
        ensureEnabled();
        try (SqlSession session = sqlSessionFactory.openSession(settings.autoCommit)) {
            work.accept(session);
        }
    }

    @Override
    public void registerMapper(Object pluginKey, String resourcePath) {
        ensureEnabled();
        validateMapperArguments(pluginKey, resourcePath);
        PluginResource plugin = requirePluginByKey(pluginKey);
        withPluginClassLoader(plugin, () -> registerMapperResource(plugin, resourcePath));
    }

    private void validateMapperArguments(Object pluginKey, String resourcePath) {
        if (pluginKey == null) {
            throw new IllegalArgumentException("Plugin key cannot be null.");
        }
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource path cannot be empty.");
        }
    }

    private PluginResource requirePluginByKey(Object pluginKey) {
        PluginResource plugin = findPluginByKey(pluginKey);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin not found for key: " + pluginKey);
        }
        return plugin;
    }

    private void withPluginClassLoader(PluginResource plugin, Runnable action) {
        ClassLoader pluginLoader = plugin.getClassLoader();
        ClassLoader previous = Resources.getDefaultClassLoader();
        boolean switched = pluginLoader != null && pluginLoader != previous;
        if (switched) {
            Resources.setDefaultClassLoader(pluginLoader);
        }
        try {
            action.run();
        } finally {
            if (switched) {
                Resources.setDefaultClassLoader(previous);
            }
        }
    }

    private void registerMapperResource(PluginResource plugin, String resourcePath) {
        try (InputStream input = mapperResourceResolver.openMapperResource(plugin, resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Mapper resource not found: " + resourcePath);
            }
            String resourceId = buildResourceId(plugin, resourcePath);
            MapperXmlLoader.registerMapper(sqlSessionFactory, resourceId, input);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to register mapper: " + resourcePath, ex);
        }
    }

    private String buildResourceId(PluginResource plugin, String resourcePath) {
        return plugin.getName() + ":" + resourcePath;
    }

    @Override
    public void registerMapper(String pluginPackage, String resourcePath) {
        PluginResource target = findPluginByPackage(pluginPackage);
        if (target == null) {
            throw new IllegalArgumentException("Plugin not found for package: " + pluginPackage);
        }
        registerMapper(target.getKey(), resourcePath);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        enabled = false;
        if (sqlSessionFactory == null) {
            return;
        }
        try {
            Object dataSource = sqlSessionFactory.getConfiguration()
                    .getEnvironment()
                    .getDataSource();
            if (dataSource instanceof AutoCloseable closeable) {
                closeable.close();
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "[MyBatis] Failed to close data source.", ex);
        } finally {
            sqlSessionFactory = null;
        }
    }

    private void registerGlobalBeans() {
        registerGlobalBean(MybatisService.class, this);
        if (sqlSessionFactory != null) {
            registerGlobalBean(SqlSessionFactory.class, sqlSessionFactory);
        }
    }

    private void registerGlobalBean(Class<?> type, Object instance) {
        try {
            Class<?> bukkitSpringClass = Class.forName("com.cuzz.bukkitspring.BukkitSpring");
            java.lang.reflect.Method registerMethod = bukkitSpringClass
                    .getMethod("registerGlobalBean", Class.class, Object.class);
            registerMethod.invoke(null, type, instance);
        } catch (Exception ex) {
            logger.warning("[MyBatis] Failed to register global bean: " + ex.getMessage());
        }
    }

    private void ensureEnabled() {
        if (!isEnabled() || sqlSessionFactory == null) {
            throw new IllegalStateException("MyBatis is not enabled or not initialized.");
        }
    }

    private PluginResource findPluginByPackage(String pluginPackage) {
        if (pluginPackage == null || pluginPackage.trim().isEmpty()) {
            return null;
        }
        for (PluginResource plugin : pluginResourceResolver.listPlugins()) {
            String pkg = plugin.getMainPackage();
            if (pkg != null && pkg.startsWith(pluginPackage)) {
                return plugin;
            }
        }
        return null;
    }

    private PluginResource findPluginByKey(Object pluginKey) {
        for (PluginResource plugin : pluginResourceResolver.listPlugins()) {
            if (Objects.equals(pluginKey, plugin.getKey())) {
                return plugin;
            }
        }
        return null;
    }
}




