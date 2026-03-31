package com.cuzz.starter.bukkitspring.prometheus.internal;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.bukkitspring.api.annotation.PreDestroy;
import com.cuzz.starter.bukkitspring.prometheus.api.PrometheusService;
import com.cuzz.starter.bukkitspring.prometheus.config.PrometheusSettings;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HttpConnectionFactory;
import io.prometheus.client.exporter.PushGateway;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public final class DefaultPrometheusService implements PrometheusService, AutoCloseable {
    private static final AtomicBoolean JVM_INITIALIZED = new AtomicBoolean(false);

    private final PrometheusSettings settings;
    private final Logger logger;
    private final CollectorRegistry registry;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean pushing = new AtomicBoolean(false);
    private final Object executorLock = new Object();
    private final Object gatewayLock = new Object();
    private volatile ScheduledExecutorService executor;
    private volatile PushGateway pushGateway;

    @Autowired
    public DefaultPrometheusService(PrometheusSettings settings, Logger logger) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = logger;
        this.registry = CollectorRegistry.defaultRegistry;
    }

    @PostConstruct
    public void registerGlobalBean() {
        if (!settings.enabled) {
            logInfo("[Prometheus] Disabled, skip global bean registration.");
            return;
        }
        registerGlobalBeanInternal();
        if (!settings.isConfigured()) {
            logWarning("[Prometheus] Missing pushgateway url or job, push disabled.");
            return;
        }
        initializeJvmExports();
        startScheduler();
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
    public CollectorRegistry registry() {
        return registry;
    }

    @Override
    public void pushOnce() {
        if (!isEnabled()) {
            logDebug("[Prometheus] Push skipped, service disabled.");
            return;
        }
        if (!settings.isConfigured()) {
            logWarning("[Prometheus] Push skipped: missing pushgateway url or job.");
            return;
        }
        doPush(true);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        ScheduledExecutorService current = executor;
        if (current != null) {
            current.shutdown();
        }
    }

    private void startScheduler() {
        if (executor != null) {
            return;
        }
        synchronized (executorLock) {
            if (executor == null) {
                executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "bukkitspring-prometheus-push");
                    thread.setDaemon(true);
                    return thread;
                });
                long interval = settings.pushIntervalMs;
                if (interval <= 0) {
                    logInfo("[Prometheus] Push scheduler disabled (push-interval-ms <= 0).");
                    return;
                }
                executor.scheduleWithFixedDelay(this::safeScheduledPush, 0, interval, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void safeScheduledPush() {
        if (!isEnabled()) {
            return;
        }
        if (!settings.isConfigured()) {
            logWarning("[Prometheus] Push skipped: missing pushgateway url or job.");
            return;
        }
        doPush(false);
    }

    private void doPush(boolean manual) {
        if (!pushing.compareAndSet(false, true)) {
            if (manual) {
                logDebug("[Prometheus] Push already in progress, skip manual push.");
            }
            return;
        }
        try {
            PushGateway gateway = ensurePushGateway();
            Map<String, String> grouping = settings.grouping;
            if (settings.pushMode == PrometheusSettings.PushMode.PUSH_ADD) {
                if (grouping.isEmpty()) {
                    gateway.pushAdd(registry, settings.job);
                } else {
                    gateway.pushAdd(registry, settings.job, grouping);
                }
            } else {
                if (grouping.isEmpty()) {
                    gateway.push(registry, settings.job);
                } else {
                    gateway.push(registry, settings.job, grouping);
                }
            }
            logDebug("[Prometheus] Push success (" + settings.pushMode + ").");
        } catch (IOException ex) {
            logWarning("[Prometheus] Push failed: " + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            logWarning("[Prometheus] Push failed: " + ex.getMessage(), ex);
        } finally {
            pushing.set(false);
        }
    }

    private PushGateway ensurePushGateway() {
        PushGateway current = pushGateway;
        if (current != null) {
            return current;
        }
        synchronized (gatewayLock) {
            if (pushGateway == null) {
                PushGateway created = new PushGateway(settings.pushGatewayUrl);
                created.setConnectionFactory(new TimeoutHttpConnectionFactory(settings.pushTimeoutMs));
                pushGateway = created;
            }
            return pushGateway;
        }
    }

    private void initializeJvmExports() {
        if (!settings.includeJvm) {
            return;
        }
        if (!JVM_INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        try {
            DefaultExports.initialize();
            logInfo("[Prometheus] JVM metrics registered.");
        } catch (Throwable ex) {
            logWarning("[Prometheus] Failed to register JVM metrics: " + ex.getMessage(), ex);
        }
    }

    private void registerGlobalBeanInternal() {
        try {
            Class<?> bukkitSpringClass = Class.forName("com.cuzz.bukkitspring.BukkitSpring");
            java.lang.reflect.Method registerMethod = bukkitSpringClass
                    .getMethod("registerGlobalBean", Class.class, Object.class);
            registerMethod.invoke(null, PrometheusService.class, this);
            logInfo("[Prometheus] PrometheusService registered as global bean");
        } catch (Exception e) {
            logWarning("[Prometheus] Failed to register global bean: " + e.getMessage());
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

    private void logWarning(String message, Throwable error) {
        if (logger == null) {
            return;
        }
        if (settings.debug && error != null) {
            logger.log(Level.WARNING, message, error);
        } else {
            logger.warning(message);
        }
    }

    private void logDebug(String message) {
        if (logger != null && settings.debug) {
            logger.info(message);
        }
    }

    private static final class TimeoutHttpConnectionFactory implements HttpConnectionFactory {
        private final int timeoutMs;

        private TimeoutHttpConnectionFactory(long timeoutMs) {
            long normalized = timeoutMs <= 0 ? 5000 : timeoutMs;
            this.timeoutMs = (int) Math.min(Integer.MAX_VALUE, normalized);
        }

        @Override
        public HttpURLConnection create(String url) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            return connection;
        }
    }
}
