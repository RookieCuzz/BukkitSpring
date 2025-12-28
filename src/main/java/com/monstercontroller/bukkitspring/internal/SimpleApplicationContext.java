package com.monstercontroller.bukkitspring.internal;

import com.monstercontroller.bukkitspring.api.ApplicationContext;
import com.monstercontroller.bukkitspring.api.Provider;
import com.monstercontroller.bukkitspring.api.annotation.Autowired;
import com.monstercontroller.bukkitspring.api.annotation.Bean;
import com.monstercontroller.bukkitspring.api.annotation.Component;
import com.monstercontroller.bukkitspring.api.annotation.Configuration;
import com.monstercontroller.bukkitspring.api.annotation.Lazy;
import com.monstercontroller.bukkitspring.api.annotation.Primary;
import com.monstercontroller.bukkitspring.api.annotation.Qualifier;
import com.monstercontroller.bukkitspring.api.annotation.Scope;
import com.monstercontroller.bukkitspring.api.annotation.ScopeType;
import com.monstercontroller.bukkitspring.api.exception.BeanCreationException;
import com.monstercontroller.bukkitspring.api.exception.BeanDefinitionException;
import com.monstercontroller.bukkitspring.api.exception.CircularDependencyException;
import com.monstercontroller.bukkitspring.api.exception.NoSuchBeanException;
import com.monstercontroller.bukkitspring.api.kafka.KafkaConsumerManager;
import com.monstercontroller.bukkitspring.api.kafka.KafkaService;
import com.monstercontroller.bukkitspring.api.redis.RedisService;
import com.monstercontroller.bukkitspring.BukkitSpring;
import com.monstercontroller.bukkitspring.kafka.KafkaListenerProcessor;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class SimpleApplicationContext implements ApplicationContext {
    private final JavaPlugin plugin;
    private final ClassLoader classLoader;
    private final Logger logger;
    private final Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();
    private final Map<String, Object> earlySingletons = new ConcurrentHashMap<>();
    private final Set<String> creating = ConcurrentHashMap.newKeySet();
    private final List<String> scannedPackages = new ArrayList<>();
    private volatile boolean refreshed = false;

    public SimpleApplicationContext(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.classLoader = plugin.getClass().getClassLoader();
        this.logger = plugin.getLogger();
        bindBuiltinInstances();
    }

    private void bindBuiltinInstances() {
        bindInstance(JavaPlugin.class, plugin);
        bindInstance(Plugin.class, plugin);
        bindInstance(Logger.class, logger);
        bindInstance(Server.class, plugin.getServer());
        bindInstance(PluginManager.class, plugin.getServer().getPluginManager());
        bindInstance(FileConfiguration.class, plugin.getConfig());
        bindInstance(BukkitScheduler.class, plugin.getServer().getScheduler());
        bindProvider(KafkaService.class, () -> {
            KafkaService service = BukkitSpring.getKafkaService();
            if (service == null) {
                throw new NoSuchBeanException("Kafka service is not initialized.");
            }
            return service;
        });
        bindProvider(KafkaConsumerManager.class, () -> {
            KafkaConsumerManager manager = BukkitSpring.getKafkaConsumerManager();
            if (manager == null) {
                throw new NoSuchBeanException("Kafka consumer manager is not initialized.");
            }
            return manager;
        });
        bindProvider(RedisService.class, () -> {
            RedisService service = BukkitSpring.getRedisService();
            if (service == null) {
                throw new NoSuchBeanException("Redis service is not initialized.");
            }
            return service;
        });
    }

    @Override
    public synchronized void scan(String... basePackages) {
        if (basePackages == null || basePackages.length == 0) {
            throw new BeanDefinitionException("At least one base package is required for scanning.");
        }
        scannedPackages.addAll(Arrays.asList(basePackages));
        try (ScanResult result = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .overrideClassLoaders(classLoader)
                .acceptPackages(basePackages)
                .scan()) {
            List<ClassInfo> components = result.getClassesWithAnnotation(Component.class.getName());
            List<ClassInfo> configurations = result.getClassesWithAnnotation(Configuration.class.getName());

            for (ClassInfo info : components) {
                registerComponentClass(info.loadClass());
            }
            for (ClassInfo info : configurations) {
                registerComponentClass(info.loadClass());
            }
        }
    }

    @Override
    public synchronized void refresh() {
        if (refreshed) {
            return;
        }
        for (BeanDefinition definition : definitions.values()) {
            if (!definition.isPrototype()) {
                getByName(definition.getName());
            }
        }
        refreshed = true;
        logger.info("[DEBUG] Context refreshed, checking KafkaConsumerManager...");
        KafkaConsumerManager manager = BukkitSpring.getKafkaConsumerManager();
        if (manager == null) {
            logger.warning("[DEBUG] KafkaConsumerManager is null, skipping listener processing");
        } else {
            logger.info("[DEBUG] KafkaConsumerManager found, starting listener processing");
            KafkaListenerProcessor processor = new KafkaListenerProcessor(manager, logger);
            try {
                processor.process(this);
                manager.startAll();
                logger.info("[DEBUG] KafkaListener processing completed");
            } catch (Exception ex) {
                logger.warning("Kafka listener processing failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        for (BeanDefinition definition : definitions.values()) {
            Object instance = singletons.get(definition.getName());
            if (instance == null) {
                continue;
            }
            Method preDestroy = definition.getPreDestroy();
            if (preDestroy == null) {
                continue;
            }
            try {
                preDestroy.setAccessible(true);
                preDestroy.invoke(instance);
            } catch (ReflectiveOperationException ex) {
                logger.warning("PreDestroy failed for bean " + definition.getName() + ": " + ex.getMessage());
            }
        }
        singletons.clear();
        earlySingletons.clear();
        creating.clear();
    }

    @Override
    public <T> T get(Class<T> type) {
        return resolveDependency(type, null, true);
    }

    @Override
    public <T> T get(Class<T> type, String qualifier) {
        return resolveDependency(type, qualifier, true);
    }

    public List<Object> getAllBeans() {
        return new ArrayList<>(singletons.values());
    }

    @Override
    public synchronized <T> void bindInstance(Class<T> type, T instance) {
        String name = BeanNames.defaultName(type);
        if (definitions.containsKey(name)) {
            throw new BeanDefinitionException("Bean name already registered: " + name);
        }
        BeanDefinition definition = new BeanDefinition(
                name,
                type,
                ScopeType.SINGLETON,
                false,
                null,
                null,
                null,
                null,
                false,
                null,
                () -> instance
        );
        registerDefinition(definition);
        singletons.put(name, instance);
    }

    @Override
    public synchronized <T> void bindProvider(Class<T> type, Provider<T> provider) {
        String name = BeanNames.defaultName(type);
        if (definitions.containsKey(name)) {
            throw new BeanDefinitionException("Bean name already registered: " + name);
        }
        BeanDefinition definition = new BeanDefinition(
                name,
                type,
                ScopeType.SINGLETON,
                false,
                null,
                null,
                null,
                null,
                false,
                null,
                provider::get
        );
        registerDefinition(definition);
    }

    private void registerComponentClass(Class<?> type) {
        if (type.isInterface() || type.isAnnotation() || Modifier.isAbstract(type.getModifiers())) {
            return;
        }
        String name = resolveComponentName(type);
        if (definitions.containsKey(name)) {
            return;
        }
        ScopeType scope = resolveScope(type.getAnnotations());
        boolean primary = type.isAnnotationPresent(Primary.class);
        Constructor<?> constructor = selectConstructor(type);
        Method postConstruct = findLifecycleMethod(type, PostConstruct.class);
        Method preDestroy = findLifecycleMethod(type, PreDestroy.class);
        BeanDefinition definition = new BeanDefinition(
                name,
                type,
                scope,
                primary,
                constructor,
                postConstruct,
                preDestroy,
                null,
                false,
                null,
                null
        );
        registerInjectionPoints(definition, type);
        registerDefinition(definition);

        if (type.isAnnotationPresent(Configuration.class)) {
            registerBeanMethods(definition, type);
        }
    }

    private void registerBeanMethods(BeanDefinition configurationDefinition, Class<?> configType) {
        for (Method method : configType.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Bean.class)) {
                continue;
            }
            if (method.getReturnType() == Void.TYPE) {
                throw new BeanDefinitionException("@Bean method must not return void: " + method);
            }
            Bean bean = method.getAnnotation(Bean.class);
            String name = bean.name().isEmpty() ? method.getName() : bean.name();
            if (definitions.containsKey(name)) {
                throw new BeanDefinitionException("Bean name already registered: " + name);
            }
            ScopeType scope = resolveScope(method.getAnnotations());
            boolean primary = method.isAnnotationPresent(Primary.class);
            boolean staticFactory = Modifier.isStatic(method.getModifiers());
            String factoryBeanName = staticFactory ? null : configurationDefinition.getName();
            BeanDefinition definition = new BeanDefinition(
                    name,
                    method.getReturnType(),
                    scope,
                    primary,
                    null,
                    null,
                    null,
                    method,
                    staticFactory,
                    factoryBeanName,
                    null
            );
            registerDefinition(definition);
        }
    }

    private void registerInjectionPoints(BeanDefinition definition, Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                definition.getInjectionPoints().add(new InjectionPoint.FieldInjectionPoint(field));
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Autowired.class)) {
                definition.getInjectionPoints().add(new InjectionPoint.MethodInjectionPoint(method));
            }
        }
    }

    private void registerDefinition(BeanDefinition definition) {
        definitions.put(definition.getName(), definition);
    }

    private String resolveComponentName(Class<?> type) {
        Component component = type.getAnnotation(Component.class);
        if (component != null && !component.value().isEmpty()) {
            return component.value();
        }
        return BeanNames.defaultName(type);
    }

    private ScopeType resolveScope(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Scope) {
                return ((Scope) annotation).value();
            }
        }
        return ScopeType.SINGLETON;
    }

    private Constructor<?> selectConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (constructors.length == 1) {
            return constructors[0];
        }
        Constructor<?> autowired = null;
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                if (autowired != null) {
                    throw new BeanDefinitionException("Multiple @Autowired constructors: " + type.getName());
                }
                autowired = constructor;
            }
        }
        if (autowired != null) {
            return autowired;
        }
        Constructor<?> best = null;
        int maxParams = -1;
        for (Constructor<?> constructor : constructors) {
            if (!Modifier.isPublic(constructor.getModifiers())) {
                continue;
            }
            int params = constructor.getParameterCount();
            if (params > maxParams) {
                maxParams = params;
                best = constructor;
            }
        }
        if (best != null) {
            return best;
        }
        throw new BeanDefinitionException("No suitable constructor found for " + type.getName());
    }

    private Method findLifecycleMethod(Class<?> type, Class<? extends Annotation> annotation) {
        Method found = null;
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                if (method.getParameterCount() != 0) {
                    throw new BeanDefinitionException("Lifecycle method must have no args: " + method);
                }
                if (found != null) {
                    throw new BeanDefinitionException("Multiple lifecycle methods found: " + annotation.getSimpleName() + " on " + type.getName());
                }
                found = method;
            }
        }
        return found;
    }

    private Object getByName(String name) {
        BeanDefinition definition = definitions.get(name);
        if (definition == null) {
            throw new NoSuchBeanException("No bean named: " + name);
        }
        if (!definition.isPrototype()) {
            Object existing = singletons.get(name);
            if (existing != null) {
                return existing;
            }
        }
        return createBean(definition);
    }

    private Object createBean(BeanDefinition definition) {
        String name = definition.getName();
        if (!definition.isPrototype()) {
            Object existing = singletons.get(name);
            if (existing != null) {
                return existing;
            }
            Object early = earlySingletons.get(name);
            if (early != null) {
                return early;
            }
        }

        if (!creating.add(name)) {
            Object early = earlySingletons.get(name);
            if (early != null) {
                return early;
            }
            throw new CircularDependencyException("Circular dependency detected while creating " + name + ": " + creating);
        }

        try {
            Object instance = instantiate(definition);
            if (!definition.isPrototype()) {
                earlySingletons.put(name, instance);
            }
            for (InjectionPoint point : definition.getInjectionPoints()) {
                point.inject(this, instance);
            }
            Method postConstruct = definition.getPostConstruct();
            if (postConstruct != null) {
                postConstruct.setAccessible(true);
                postConstruct.invoke(instance);
            }
            if (!definition.isPrototype()) {
                earlySingletons.remove(name);
                singletons.put(name, instance);
            }
            return instance;
        } catch (BeanCreationException ex) {
            throw ex;
        } catch (ReflectiveOperationException ex) {
            throw new BeanCreationException("Failed to create bean " + name, ex);
        } finally {
            creating.remove(name);
        }
    }

    private Object instantiate(BeanDefinition definition) throws ReflectiveOperationException {
        Supplier<?> supplier = definition.getInstanceSupplier();
        if (supplier != null) {
            return supplier.get();
        }
        Method factory = definition.getFactoryMethod();
        if (factory != null) {
            Object target = null;
            if (!definition.isFactoryMethodStatic()) {
                target = getByName(definition.getFactoryBeanName());
            }
            Object[] args = resolveExecutableArguments(factory, factory.getParameters(), factory.getGenericParameterTypes());
            factory.setAccessible(true);
            return factory.invoke(target, args);
        }
        Constructor<?> constructor = definition.getConstructor();
        Object[] args = resolveExecutableArguments(constructor, constructor.getParameters(), constructor.getGenericParameterTypes());
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }

    Object[] resolveExecutableArguments(Object source, Parameter[] parameters, Type[] genericTypes) {
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            args[i] = resolveDependency(parameter, parameter.getType(), genericTypes[i]);
        }
        return args;
    }

    Object resolveDependency(AnnotatedElement element, Class<?> type, Type genericType) {
        String qualifier = extractQualifier(element);
        boolean required = isRequired(element);
        boolean lazy = element.isAnnotationPresent(Lazy.class);

        if (Provider.class.isAssignableFrom(type) || Supplier.class.isAssignableFrom(type)) {
            Class<?> targetType = resolveGenericType(genericType, element);
            if (Provider.class.isAssignableFrom(type)) {
                return (Provider<Object>) () -> resolveOptional(targetType, qualifier, required);
            }
            return (Supplier<Object>) () -> resolveOptional(targetType, qualifier, required);
        }

        if (lazy) {
            if (!type.isInterface()) {
                throw new BeanCreationException("@Lazy injection requires an interface type: " + type.getName());
            }
            Supplier<Object> supplier = () -> resolveOptional(type, qualifier, required);
            return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
                Object target = supplier.get();
                if (target == null) {
                    return null;
                }
                return method.invoke(target, args);
            });
        }

        return resolveOptional(type, qualifier, required);
    }

    private <T> T resolveDependency(Class<T> type, String qualifier, boolean required) {
        return resolveOptional(type, qualifier, required);
    }

    @SuppressWarnings("unchecked")
    private <T> T resolveOptional(Class<T> type, String qualifier, boolean required) {
        try {
            if (qualifier != null && !qualifier.isEmpty()) {
                Object bean = getByName(qualifier);
                if (!type.isInstance(bean)) {
                    throw new BeanCreationException("Bean " + qualifier + " is not of type " + type.getName());
                }
                return (T) bean;
            }
            List<BeanDefinition> candidates = findCandidates(type);
            if (candidates.isEmpty()) {
                if (!required) {
                    return null;
                }
                throw new NoSuchBeanException("No bean found for type: " + type.getName());
            }
            if (candidates.size() == 1) {
                return (T) getByName(candidates.get(0).getName());
            }
            BeanDefinition primary = null;
            for (BeanDefinition candidate : candidates) {
                if (candidate.isPrimary()) {
                    if (primary != null) {
                        throw new BeanCreationException("Multiple @Primary beans for type " + type.getName());
                    }
                    primary = candidate;
                }
            }
            if (primary != null) {
                return (T) getByName(primary.getName());
            }
            throw new BeanCreationException("Multiple beans found for type " + type.getName() + ", use @Qualifier or @Primary");
        } catch (NoSuchBeanException ex) {
            if (!required) {
                return null;
            }
            throw ex;
        }
    }

    private List<BeanDefinition> findCandidates(Class<?> type) {
        List<BeanDefinition> matches = new ArrayList<>();
        for (BeanDefinition definition : definitions.values()) {
            if (type.isAssignableFrom(definition.getType())) {
                matches.add(definition);
            }
        }
        return matches;
    }

    private String extractQualifier(AnnotatedElement element) {
        Qualifier qualifier = element.getAnnotation(Qualifier.class);
        if (qualifier != null && !qualifier.value().isEmpty()) {
            return qualifier.value();
        }
        return null;
    }

    private boolean isRequired(AnnotatedElement element) {
        Autowired autowired = element.getAnnotation(Autowired.class);
        return autowired == null || autowired.required();
    }

    private Class<?> resolveGenericType(Type genericType, AnnotatedElement element) {
        if (genericType instanceof ParameterizedType) {
            Type actual = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            if (actual instanceof Class) {
                return (Class<?>) actual;
            }
        }
        throw new BeanCreationException("Provider/Supplier must declare generic type: " + element);
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public Logger getLogger() {
        return logger;
    }

    public List<String> getScannedPackages() {
        return List.copyOf(scannedPackages);
    }
}
