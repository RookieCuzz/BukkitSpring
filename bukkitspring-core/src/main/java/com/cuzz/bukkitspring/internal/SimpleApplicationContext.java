package com.cuzz.bukkitspring.internal;

import com.cuzz.bukkitspring.api.ApplicationContext;
import com.cuzz.bukkitspring.api.BeanPostProcessor;
import com.cuzz.bukkitspring.api.ObjectFactory;
import com.cuzz.bukkitspring.api.Provider;
import com.cuzz.bukkitspring.api.SmartInstantiationAwareBeanPostProcessor;
import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Bean;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.Configuration;
import com.cuzz.bukkitspring.api.annotation.Controller;
import com.cuzz.bukkitspring.api.annotation.Lazy;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.bukkitspring.api.annotation.PreDestroy;
import com.cuzz.bukkitspring.api.annotation.Primary;
import com.cuzz.bukkitspring.api.annotation.Qualifier;
import com.cuzz.bukkitspring.api.annotation.Repository;
import com.cuzz.bukkitspring.api.annotation.Scope;
import com.cuzz.bukkitspring.api.annotation.ScopeType;
import com.cuzz.bukkitspring.api.annotation.Service;
import com.cuzz.bukkitspring.api.exception.BeanCreationException;
import com.cuzz.bukkitspring.api.exception.BeanDefinitionException;
import com.cuzz.bukkitspring.api.exception.CircularDependencyException;
import com.cuzz.bukkitspring.api.exception.NoSuchBeanException;
import com.cuzz.bukkitspring.spi.config.ConfigView;
import com.cuzz.bukkitspring.spi.platform.PlatformContext;
import com.cuzz.bukkitspring.spi.platform.PlatformScheduler;
import com.cuzz.bukkitspring.spi.platform.PluginResourceResolver;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.nio.file.Path;

public final class SimpleApplicationContext implements ApplicationContext {
    private final PlatformContext platformContext;
    private final ClassLoader classLoader;
    private final Logger logger;
    private final Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();
    private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet();
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    private volatile boolean postProcessorsInitialized = false;
    private boolean initializingPostProcessors = false;
    private final List<String> scannedPackages = new ArrayList<>();
    private volatile boolean refreshed = false;

    public SimpleApplicationContext(PlatformContext platformContext) {
        this.platformContext = Objects.requireNonNull(platformContext, "platformContext");
        this.classLoader = platformContext.getClassLoader();
        this.logger = platformContext.getLogger();
        bindBuiltinInstances();
    }

    private void bindBuiltinInstances() {
        if (logger != null) {
            bindIfAbsent(Logger.class, logger);
        }
        ConfigView configView = platformContext.getConfig();
        if (configView != null) {
            bindIfAbsent(ConfigView.class, configView);
        }
        Path dataDirectory = platformContext.getDataDirectory();
        if (dataDirectory != null) {
            bindIfAbsent(Path.class, dataDirectory);
        }
        PluginResourceResolver resolver = platformContext.getPluginResourceResolver();
        if (resolver != null) {
            bindIfAbsent(PluginResourceResolver.class, resolver);
        }
        PlatformScheduler scheduler = platformContext.getScheduler();
        if (scheduler != null) {
            bindIfAbsent(PlatformScheduler.class, scheduler);
        }

        if (platformContext.getBuiltinBeans() != null) {
            for (Map.Entry<Class<?>, Object> entry : platformContext.getBuiltinBeans().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String name = BeanNames.defaultName(entry.getKey());
                if (definitions.containsKey(name)) {
                    continue;
                }
                bindInstance((Class<Object>) entry.getKey(), entry.getValue());
            }
        }

        // ?????????? Bean????????????????? Starter??
        bindAllGlobalBeans();
    }

    private <T> void bindIfAbsent(Class<T> type, T instance) {
        if (type == null || instance == null) {
            return;
        }
        String name = BeanNames.defaultName(type);
        if (definitions.containsKey(name)) {
            return;
        }
        bindInstance(type, instance);
    }

    private void bindAllGlobalBeans() {
        Map<Class<?>, Object> globalBeans = com.cuzz.bukkitspring.BukkitSpring.getAllGlobalBeans();
        if (!globalBeans.isEmpty()) {
            for (Map.Entry<Class<?>, Object> entry : globalBeans.entrySet()) {
                String name = BeanNames.defaultName(entry.getKey());
                if (definitions.containsKey(name)) {
                    continue;
                }
                bindInstance((Class<Object>) entry.getKey(), entry.getValue());
                logger.fine("[BukkitSpring] Bound global bean: " + entry.getKey().getName());
            }
        }
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
            Set<ClassInfo> components = new LinkedHashSet<>();
            components.addAll(result.getClassesWithAnnotation(Component.class.getName()));
            components.addAll(result.getClassesWithAnnotation(Service.class.getName()));
            components.addAll(result.getClassesWithAnnotation(Repository.class.getName()));
            components.addAll(result.getClassesWithAnnotation(Controller.class.getName()));
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
        bindAllGlobalBeans();
        registerBeanPostProcessors();
        for (BeanDefinition definition : definitions.values()) {
            if (!definition.isPrototype()) {
                getByName(definition.getName());
            }
        }
        refreshed = true;
    }

    @Override
    public void close() {
        for (BeanDefinition definition : definitions.values()) {
            Object instance = singletonObjects.get(definition.getName());
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
        singletonObjects.clear();
        earlySingletonObjects.clear();
        singletonFactories.clear();
        singletonsCurrentlyInCreation.clear();
        beanPostProcessors.clear();
        postProcessorsInitialized = false;
        initializingPostProcessors = false;
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
        return new ArrayList<>(singletonObjects.values());
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
        singletonObjects.put(name, instance);
        if (instance instanceof BeanPostProcessor) {
            addBeanPostProcessor((BeanPostProcessor) instance);
        }
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
        Service service = type.getAnnotation(Service.class);
        if (service != null && !service.value().isEmpty()) {
            return service.value();
        }
        Repository repository = type.getAnnotation(Repository.class);
        if (repository != null && !repository.value().isEmpty()) {
            return repository.value();
        }
        Controller controller = type.getAnnotation(Controller.class);
        if (controller != null && !controller.value().isEmpty()) {
            return controller.value();
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
            if (!method.isAnnotationPresent(annotation)) {
                continue;
            }
            if (method.getParameterCount() != 0) {
                throw new BeanDefinitionException("Lifecycle method must have no args: " + method);
            }
            if (found != null) {
                throw new BeanDefinitionException("Multiple lifecycle methods found: " + annotation.getSimpleName() + " on " + type.getName());
            }
            found = method;
        }
        return found;
    }

    private void registerBeanPostProcessors() {
        if (postProcessorsInitialized || initializingPostProcessors) {
            return;
        }
        initializingPostProcessors = true;
        try {
            List<BeanDefinition> candidates = findCandidates(BeanPostProcessor.class);
            for (BeanDefinition candidate : candidates) {
                Object instance = getByName(candidate.getName());
                if (instance instanceof BeanPostProcessor) {
                    addBeanPostProcessor((BeanPostProcessor) instance);
                }
            }
        } finally {
            initializingPostProcessors = false;
            postProcessorsInitialized = true;
        }
    }

    private void addBeanPostProcessor(BeanPostProcessor processor) {
        if (!beanPostProcessors.contains(processor)) {
            beanPostProcessors.add(processor);
        }
    }

    private boolean shouldApplyPostProcessors() {
        return !initializingPostProcessors && !beanPostProcessors.isEmpty();
    }

    private Object getByName(String name) {
        BeanDefinition definition = definitions.get(name);
        if (definition == null) {
            throw new NoSuchBeanException("No bean named: " + name);
        }
        if (!definition.isPrototype()) {
            Object existing = getSingleton(name, true);
            if (existing != null) {
                return existing;
            }
        }
        return createBean(definition);
    }

    private Object getSingleton(String name, boolean allowEarlyReference) {
        Object singleton = singletonObjects.get(name);
        if (singleton != null) {
            return singleton;
        }
        if (allowEarlyReference && singletonsCurrentlyInCreation.contains(name)) {
            Object early = earlySingletonObjects.get(name);
            if (early != null) {
                return early;
            }
            ObjectFactory<?> factory = singletonFactories.get(name);
            if (factory != null) {
                Object earlyReference = factory.getObject();
                earlySingletonObjects.put(name, earlyReference);
                singletonFactories.remove(name);
                return earlyReference;
            }
        }
        return null;
    }

    private void addSingletonFactory(String name, ObjectFactory<?> factory) {
        if (!singletonObjects.containsKey(name)) {
            singletonFactories.put(name, factory);
            earlySingletonObjects.remove(name);
        }
    }

    private void addSingleton(String name, Object singleton) {
        singletonObjects.put(name, singleton);
        singletonFactories.remove(name);
        earlySingletonObjects.remove(name);
    }

    private Object createBean(BeanDefinition definition) {
        String name = definition.getName();
        if (!definition.isPrototype()) {
            Object existing = getSingleton(name, false);
            if (existing != null) {
                return existing;
            }
        }

        if (!singletonsCurrentlyInCreation.add(name)) {
            Object early = getSingleton(name, true);
            if (early != null) {
                return early;
            }
            throw new CircularDependencyException("Circular dependency detected while creating " + name + ": " + singletonsCurrentlyInCreation);
        }

        try {
            Object instance = instantiate(definition);
            if (!definition.isPrototype()) {
                addSingletonFactory(name, () -> getEarlyBeanReference(name, instance));
            }
            for (InjectionPoint point : definition.getInjectionPoints()) {
                point.inject(this, instance);
            }
            Object initialized = initializeBean(definition, instance);
            if (!definition.isPrototype()) {
                Object early = earlySingletonObjects.get(name);
                Object exposed = early != null ? early : initialized;
                addSingleton(name, exposed);
                return exposed;
            }
            return initialized;
        } catch (BeanCreationException ex) {
            throw ex;
        } catch (ReflectiveOperationException ex) {
            throw new BeanCreationException("Failed to create bean " + name, ex);
        } finally {
            singletonsCurrentlyInCreation.remove(name);
            singletonFactories.remove(name);
            earlySingletonObjects.remove(name);
        }
    }

    private Object getEarlyBeanReference(String name, Object instance) {
        Object result = instance;
        if (!shouldApplyPostProcessors()) {
            return result;
        }
        for (BeanPostProcessor processor : beanPostProcessors) {
            if (processor instanceof SmartInstantiationAwareBeanPostProcessor) {
                Object processed = ((SmartInstantiationAwareBeanPostProcessor) processor)
                        .getEarlyBeanReference(result, name);
                if (processed != null) {
                    result = processed;
                }
            }
        }
        return result;
    }

    private Object initializeBean(BeanDefinition definition, Object instance) throws ReflectiveOperationException {
        String name = definition.getName();
        Object result = instance;
        if (shouldApplyPostProcessors()) {
            result = applyBeanPostProcessorsBeforeInitialization(result, name);
        }
        Method postConstruct = definition.getPostConstruct();
        if (postConstruct != null) {
            postConstruct.setAccessible(true);
            postConstruct.invoke(instance);
        }
        if (shouldApplyPostProcessors()) {
            result = applyBeanPostProcessorsAfterInitialization(result, name);
        }
        return result;
    }

    private Object applyBeanPostProcessorsBeforeInitialization(Object existing, String name) {
        Object result = existing;
        for (BeanPostProcessor processor : beanPostProcessors) {
            Object processed = processor.postProcessBeforeInitialization(result, name);
            if (processed != null) {
                result = processed;
            }
        }
        return result;
    }

    private Object applyBeanPostProcessorsAfterInitialization(Object existing, String name) {
        Object result = existing;
        for (BeanPostProcessor processor : beanPostProcessors) {
            Object processed = processor.postProcessAfterInitialization(result, name);
            if (processed != null) {
                result = processed;
            }
        }
        return result;
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

    public PlatformContext getPlatformContext() {
        return platformContext;
    }

    public Logger getLogger() {
        return logger;
    }

    public List<String> getScannedPackages() {
        return List.copyOf(scannedPackages);
    }
}
