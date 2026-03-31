package com.cuzz.starter.bukkitspring.config.internal;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.bukkitspring.api.annotation.PreDestroy;
import com.cuzz.bukkitspring.spi.platform.PluginResource;
import com.cuzz.bukkitspring.spi.platform.PluginResourceResolver;
import com.cuzz.starter.bukkitspring.config.api.ConfigDocument;
import com.cuzz.starter.bukkitspring.config.api.ConfigLoadStrategy;
import com.cuzz.starter.bukkitspring.config.api.ConfigParser;
import com.cuzz.starter.bukkitspring.config.api.ConfigQuery;
import com.cuzz.starter.bukkitspring.config.api.ConfigService;
import com.cuzz.starter.bukkitspring.config.config.ConfigStarterSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public final class DefaultConfigService implements ConfigService {
    private final ConfigStarterSettings settings;
    private final Logger logger;
    private final Path dataDirectory;
    private final PluginResourceResolver pluginResourceResolver;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ArrayDeque<String> cacheOrder = new ArrayDeque<>();
    private final Object cacheLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile PluginResource selfPluginResource;

    @Autowired
    public DefaultConfigService(ConfigStarterSettings settings,
                                Logger logger,
                                Path dataDirectory,
                                @Autowired(required = false) PluginResourceResolver pluginResourceResolver) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.pluginResourceResolver = pluginResourceResolver;
    }

    @PostConstruct
    public void registerGlobalBean() {
        if (!settings.enabled) {
            logInfo("[ConfigStarter] Disabled, skip global bean registration.");
            return;
        }
        registerGlobalBeanInternal();
    }

    @PreDestroy
    public void preDestroy() {
        close();
    }

    @Override
    public boolean isEnabled() {
        return settings.enabled && !closed.get();
    }

    @Override
    public ConfigDocument load(String name) {
        return load(ConfigQuery.of(name));
    }

    @Override
    public ConfigDocument load(ConfigQuery query) {
        ensureEnabled();
        if (query == null) {
            throw new IllegalArgumentException("Config query cannot be null.");
        }
        ConfigQuery safeQuery = query;
        ConfigLoadStrategy effectiveStrategy = safeQuery.strategy() == null ? settings.defaultStrategy : safeQuery.strategy();
        String cacheKey = safeQuery.cacheKey(effectiveStrategy);
        long now = System.currentTimeMillis();

        if (!safeQuery.bypassCache()) {
            ConfigDocument cached = getCached(cacheKey, now);
            if (cached != null) {
                return cached;
            }
        }

        ConfigDocument loaded = loadWithStrategy(safeQuery, effectiveStrategy);
        if (loaded == null) {
            throw new IllegalStateException("Failed to load config: " + safeQuery.name()
                    + ", strategy=" + effectiveStrategy);
        }

        if (!safeQuery.bypassCache()) {
            putCache(cacheKey, loaded, now);
        }
        return loaded;
    }

    @Override
    public <T> T load(ConfigQuery query, ConfigParser<T> parser) {
        if (parser == null) {
            throw new IllegalArgumentException("Config parser cannot be null.");
        }
        ConfigDocument document = load(query);
        try {
            return parser.parse(document);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse config: " + document.name(), ex);
        }
    }

    @Override
    public void invalidate(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String normalizedName = normalize(name);
        synchronized (cacheLock) {
            cacheOrder.removeIf(key -> key.startsWith(normalizedName + "|"));
            cache.keySet().removeIf(key -> key.startsWith(normalizedName + "|"));
        }
    }

    @Override
    public void invalidateAll() {
        synchronized (cacheLock) {
            cache.clear();
            cacheOrder.clear();
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        invalidateAll();
    }

    private ConfigDocument loadWithStrategy(ConfigQuery query, ConfigLoadStrategy strategy) {
        return switch (strategy) {
            case LOCAL_ONLY -> loadLocal(query);
            case REMOTE_ONLY -> loadRemote(query);
            case LOCAL_FIRST -> {
                ConfigDocument local = loadLocal(query);
                yield local == null ? loadRemote(query) : local;
            }
            case REMOTE_FIRST -> {
                ConfigDocument remote = loadRemote(query);
                yield remote == null ? loadLocal(query) : remote;
            }
        };
    }

    private ConfigDocument loadLocal(ConfigQuery query) {
        if (!settings.localEnabled) {
            return null;
        }
        Path localBaseDirectory = resolveLocalBaseDirectory(query);
        if (localBaseDirectory == null) {
            return null;
        }
        Path file = resolveSafePath(localBaseDirectory, query.name());
        if (file == null) {
            return null;
        }
        try {
            if (!Files.exists(file) && settings.localBootstrapFromResource) {
                try {
                    copyDefaultResourceIfExists(query, file);
                } catch (UncheckedIOException ex) {
                    logWarning("[ConfigStarter] Failed to bootstrap local config from resource: "
                            + query.name() + ", reason=" + ex.getMessage());
                    logFine("Local bootstrap failure details.", ex);
                }
            }
            if (!Files.exists(file)) {
                return null;
            }
            String text = Files.readString(file, settings.localCharset);
            return new ConfigDocument(
                    query.name(),
                    text,
                    "local-file",
                    System.currentTimeMillis(),
                    settings.localCharset
            );
        } catch (IOException ex) {
            logWarning("[ConfigStarter] Failed to read local config " + query.name() + ": " + ex.getMessage());
            logFine("Local config read failure details.", ex);
            return null;
        }
    }

    private ConfigDocument loadRemote(ConfigQuery query) {
        if (!settings.remoteEnabled) {
            return null;
        }
        if (!"http".equalsIgnoreCase(settings.remoteProvider)) {
            logWarning("[ConfigStarter] Unsupported remote provider: " + settings.remoteProvider);
            return null;
        }
        if (settings.remoteBaseUrl == null || settings.remoteBaseUrl.isBlank()) {
            return null;
        }

        URI uri = buildRemoteUri(query.remoteName());
        if (uri == null) {
            return null;
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(settings.remoteConnectTimeoutMillis))
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofMillis(settings.remoteReadTimeoutMillis));
        for (Map.Entry<String, String> entry : settings.remoteHeaders.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }

        try {
            HttpResponse<String> response = client.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString(settings.remoteCharset)
            );
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                String message = "[ConfigStarter] Remote config request failed. name=" + query.remoteName()
                        + ", status=" + statusCode + ", uri=" + uri;
                if (settings.remoteFailOnHttpError) {
                    throw new IllegalStateException(message);
                }
                logWarning(message);
                return null;
            }

            return new ConfigDocument(
                    query.name(),
                    response.body(),
                    "remote-http",
                    System.currentTimeMillis(),
                    settings.remoteCharset
            );
        } catch (IOException ex) {
            logWarning("[ConfigStarter] Remote config request IO error. name=" + query.remoteName()
                    + ", uri=" + uri + ", reason=" + ex.getMessage());
            logFine("Remote config IO failure details.", ex);
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logWarning("[ConfigStarter] Remote config request interrupted. name=" + query.remoteName());
            return null;
        }
    }

    private URI buildRemoteUri(String remoteName) {
        String normalizedRemoteName = normalizeOrDefault(remoteName, "");
        String path = settings.remotePathTemplate.replace("{name}", normalizedRemoteName);
        String normalizedPath = normalizeOrDefault(path, normalizedRemoteName);
        String normalizedBase = settings.remoteBaseUrl.endsWith("/")
                ? settings.remoteBaseUrl.substring(0, settings.remoteBaseUrl.length() - 1)
                : settings.remoteBaseUrl;
        String finalUrl;
        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            finalUrl = normalizedPath;
        } else {
            String suffix = normalizedPath.startsWith("/") ? normalizedPath : ("/" + normalizedPath);
            finalUrl = normalizedBase + suffix;
        }
        try {
            return new URI(finalUrl);
        } catch (URISyntaxException ex) {
            logWarning("[ConfigStarter] Invalid remote config URI: " + finalUrl);
            logFine("Remote URI parse failure details.", ex);
            return null;
        }
    }

    private ConfigDocument getCached(String cacheKey, long now) {
        if (!settings.cacheEnabled || settings.cacheTtlMillis <= 0L) {
            return null;
        }
        CacheEntry entry = cache.get(cacheKey);
        if (entry == null) {
            return null;
        }
        if (now - entry.cachedAtMillis > settings.cacheTtlMillis) {
            removeCacheKey(cacheKey);
            return null;
        }
        return entry.document;
    }

    private void putCache(String cacheKey, ConfigDocument document, long now) {
        if (!settings.cacheEnabled || settings.cacheTtlMillis <= 0L || settings.cacheMaxEntries <= 0) {
            return;
        }
        cache.put(cacheKey, new CacheEntry(document, now));
        synchronized (cacheLock) {
            cacheOrder.remove(cacheKey);
            cacheOrder.addLast(cacheKey);
            while (cacheOrder.size() > settings.cacheMaxEntries) {
                String eldest = cacheOrder.pollFirst();
                if (eldest != null) {
                    cache.remove(eldest);
                }
            }
        }
    }

    private void removeCacheKey(String cacheKey) {
        synchronized (cacheLock) {
            cache.remove(cacheKey);
            cacheOrder.remove(cacheKey);
        }
    }

    private void copyDefaultResourceIfExists(ConfigQuery query, Path targetFile) {
        InputStream stream = openBundledResource(query, query.name());
        if (stream == null) {
            return;
        }
        try (InputStream input = stream) {
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private InputStream openBundledResource(ConfigQuery query, String path) {
        PluginResource resource = resolvePluginResource(query.pluginName());
        if (resource != null) {
            InputStream input = resource.openResource(path);
            if (input != null) {
                return input;
            }
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            InputStream input = contextClassLoader.getResourceAsStream(path);
            if (input != null) {
                return input;
            }
        }
        return DefaultConfigService.class.getClassLoader().getResourceAsStream(path);
    }

    private Path resolveLocalBaseDirectory(ConfigQuery query) {
        if (query == null) {
            return dataDirectory;
        }
        String pluginName = normalize(query.pluginName());
        if (pluginName.isEmpty()) {
            return dataDirectory;
        }
        PluginResource pluginResource = resolvePluginResource(pluginName);
        if (pluginResource == null || pluginResource.getDataDirectory() == null) {
            logWarning("[ConfigStarter] Plugin not found for local config load: " + pluginName);
            return null;
        }
        return pluginResource.getDataDirectory();
    }

    private PluginResource resolvePluginResource(String pluginName) {
        String normalizedPluginName = normalize(pluginName);
        if (normalizedPluginName.isEmpty()) {
            return resolveSelfPluginResource();
        }
        if (pluginResourceResolver == null) {
            return null;
        }
        try {
            List<PluginResource> resources = pluginResourceResolver.listPlugins();
            if (resources == null || resources.isEmpty()) {
                return null;
            }
            for (PluginResource resource : resources) {
                if (resource == null || resource.getName() == null) {
                    continue;
                }
                if (normalizedPluginName.equalsIgnoreCase(resource.getName().trim())) {
                    return resource;
                }
            }
        } catch (Exception ex) {
            logFine("Failed to resolve plugin resource by name.", ex);
        }
        return null;
    }

    private PluginResource resolveSelfPluginResource() {
        PluginResource cached = selfPluginResource;
        if (cached != null) {
            return cached;
        }
        if (pluginResourceResolver == null || dataDirectory == null) {
            return null;
        }
        try {
            Path normalizedDataDirectory = dataDirectory.toAbsolutePath().normalize();
            List<PluginResource> resources = pluginResourceResolver.listPlugins();
            if (resources == null || resources.isEmpty()) {
                return null;
            }
            for (PluginResource resource : resources) {
                if (resource == null || resource.getDataDirectory() == null) {
                    continue;
                }
                Path candidate = resource.getDataDirectory().toAbsolutePath().normalize();
                if (candidate.equals(normalizedDataDirectory)) {
                    selfPluginResource = resource;
                    return resource;
                }
            }
        } catch (Exception ex) {
            logFine("Failed to resolve plugin resource for config bootstrap.", ex);
        }
        return null;
    }

    private Path resolveSafePath(Path baseDir, String relativePath) {
        String normalized = normalize(relativePath);
        if (normalized.isEmpty()) {
            return null;
        }
        Path base = baseDir.toAbsolutePath().normalize();
        Path resolved = base.resolve(normalized).normalize();
        if (!resolved.startsWith(base)) {
            logWarning("[ConfigStarter] Blocked path traversal attempt: " + relativePath);
            return null;
        }
        return resolved;
    }

    private void ensureEnabled() {
        if (!settings.enabled) {
            throw new IllegalStateException("Config starter is disabled (config-starter.enabled=false).");
        }
        if (closed.get()) {
            throw new IllegalStateException("Config service is closed.");
        }
    }

    private void registerGlobalBeanInternal() {
        try {
            Class<?> bukkitSpringClass = Class.forName("com.cuzz.bukkitspring.BukkitSpring");
            java.lang.reflect.Method registerMethod = bukkitSpringClass
                    .getMethod("registerGlobalBean", Class.class, Object.class);
            registerMethod.invoke(null, ConfigService.class, this);
            logInfo("[ConfigStarter] ConfigService registered as global bean");
        } catch (Exception ex) {
            logWarning("[ConfigStarter] Failed to register global bean: " + ex.getMessage());
            logFine("Global bean registration failure details.", ex);
        }
    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    private void logWarning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    private void logFine(String message, Throwable throwable) {
        if (logger != null) {
            logger.log(Level.FINE, "[ConfigStarter] " + message, throwable);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String normalizeOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized;
    }

    private record CacheEntry(ConfigDocument document, long cachedAtMillis) {
    }
}
