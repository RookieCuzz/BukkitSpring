package com.cuzz.starter.bukkitspring.time.internal;

import com.cuzz.bukkitspring.api.annotation.Autowired;
import com.cuzz.bukkitspring.api.annotation.Component;
import com.cuzz.bukkitspring.api.annotation.PostConstruct;
import com.cuzz.bukkitspring.api.annotation.PreDestroy;
import com.cuzz.starter.bukkitspring.time.api.TimeService;
import com.cuzz.starter.bukkitspring.time.api.TimeSetResult;
import com.cuzz.starter.bukkitspring.time.config.TimeSettings;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

@Component
public final class DefaultTimeService implements TimeService {
    private final TimeSettings settings;
    private final Logger logger;
    private final Clock clock;
    private final SystemTimeCommandRunner commandRunner;
    private final String osName;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicLong debugOffsetMillis;
    private final Object executorLock = new Object();
    private volatile ExecutorService executor;

    @Autowired
    public DefaultTimeService(TimeSettings settings, Logger logger) {
        this(settings, logger, new ProcessSystemTimeCommandRunner(), System.getProperty("os.name", "unknown"));
    }

    DefaultTimeService(TimeSettings settings,
                       Logger logger,
                       SystemTimeCommandRunner commandRunner,
                       String osName) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.logger = logger;
        this.clock = Clock.system(settings.zoneId);
        this.commandRunner = Objects.requireNonNull(commandRunner, "commandRunner");
        this.osName = osName == null ? "unknown" : osName;
        this.debugOffsetMillis = new AtomicLong(settings.debugOffsetMillis);
    }

    @PostConstruct
    public void registerGlobalBean() {
        if (!settings.enabled) {
            logInfo("[Time] Disabled, skip global bean registration.");
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
    public TimeSettings settings() {
        return settings;
    }

    @Override
    public ExecutorService executor() {
        if (closed.get()) {
            throw new IllegalStateException("Time service is closed.");
        }
        ExecutorService current = executor;
        if (current != null) {
            return current;
        }
        synchronized (executorLock) {
            if (executor == null) {
                executor = TimeExecutors.create(settings.useVirtualThreads, logger);
            }
            return executor;
        }
    }

    @Override
    public Instant now() {
        ensureEnabled();
        return Instant.now(clock).plusMillis(debugOffsetMillis.get());
    }

    @Override
    public long currentTimeMillis() {
        return now().toEpochMilli();
    }

    @Override
    public ZonedDateTime now(ZoneId zoneId) {
        ensureEnabled();
        ZoneId zone = zoneId == null ? settings.zoneId : zoneId;
        return ZonedDateTime.ofInstant(now(), zone);
    }

    @Override
    public String formatNow(String pattern, String zoneId) {
        ensureEnabled();
        String normalizedPattern = normalizeOrDefault(pattern, "yyyy-MM-dd HH:mm:ss");
        ZoneId zone = resolveZoneId(zoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(normalizedPattern);
        return formatter.format(now(zone));
    }

    @Override
    public Duration debugOffset() {
        ensureEnabled();
        return Duration.ofMillis(debugOffsetMillis.get());
    }

    @Override
    public void setDebugOffset(Duration offset) {
        ensureEnabled();
        long value = offset == null ? 0L : offset.toMillis();
        debugOffsetMillis.set(value);
    }

    @Override
    public TimeSetResult setSystemTime(Instant target) {
        ensureEnabled();
        if (target == null) {
            return TimeSetResult.failure("Target time cannot be null.", -1, "", "", "", null);
        }
        if (!settings.allowSetSystemTime) {
            return TimeSetResult.failure(
                    "System time adjustment is disabled. Set time.system-time.allow-set=true to enable.",
                    -1,
                    "",
                    "",
                    "",
                    target
            );
        }

        List<List<String>> commands = SystemTimeCommandBuilder.buildCommands(
                target,
                settings.zoneId,
                settings.preferTimedatectl,
                osName
        );
        if (commands.isEmpty()) {
            return TimeSetResult.failure("No valid system time command generated.", -1, "", "", "", target);
        }

        TimeSetResult lastFailure = TimeSetResult.failure("No command executed.", -1, "", "", "", target);
        for (List<String> command : commands) {
            String joinedCommand = String.join(" ", command);
            try {
                SystemTimeCommandRunner.CommandResult result = commandRunner.run(command, settings.commandTimeoutMillis);
                if (result.timedOut) {
                    lastFailure = TimeSetResult.failure(
                            "System time command timed out.",
                            result.exitCode,
                            joinedCommand,
                            safe(result.stdout),
                            safe(result.stderr),
                            target
                    );
                    continue;
                }
                if (result.exitCode == 0) {
                    return TimeSetResult.success(
                            "System time command executed.",
                            result.exitCode,
                            joinedCommand,
                            safe(result.stdout),
                            safe(result.stderr),
                            target
                    );
                }
                lastFailure = TimeSetResult.failure(
                        "System time command failed.",
                        result.exitCode,
                        joinedCommand,
                        safe(result.stdout),
                        safe(result.stderr),
                        target
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                lastFailure = TimeSetResult.failure(
                        "System time command execution interrupted: " + e.getMessage(),
                        -1,
                        joinedCommand,
                        "",
                        "",
                        target
                );
                break;
            } catch (IOException e) {
                lastFailure = TimeSetResult.failure(
                        "System time command execution error: " + e.getMessage(),
                        -1,
                        joinedCommand,
                        "",
                        "",
                        target
                );
                break;
            } catch (RuntimeException e) {
                lastFailure = TimeSetResult.failure(
                        "System time command execution error: " + e.getMessage(),
                        -1,
                        joinedCommand,
                        "",
                        "",
                        target
                );
            }
        }
        return lastFailure;
    }

    @Override
    public CompletableFuture<TimeSetResult> setSystemTimeAsync(Instant target) {
        ensureEnabled();
        return CompletableFuture.supplyAsync(() -> setSystemTime(target), executor());
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        ExecutorService current = executor;
        if (current != null) {
            current.shutdown();
        }
    }

    private void ensureEnabled() {
        if (!settings.enabled) {
            throw new IllegalStateException("Time starter is disabled (time.enabled=false).");
        }
        if (closed.get()) {
            throw new IllegalStateException("Time service is closed.");
        }
    }

    private ZoneId resolveZoneId(String zoneId) {
        String value = normalize(zoneId);
        if (value.isEmpty()) {
            return settings.zoneId;
        }
        try {
            return ZoneId.of(value);
        } catch (Exception ignored) {
            return settings.zoneId;
        }
    }

    private void registerGlobalBeanInternal() {
        try {
            Class<?> bukkitSpringClass = Class.forName("com.cuzz.bukkitspring.BukkitSpring");
            java.lang.reflect.Method registerMethod = bukkitSpringClass
                    .getMethod("registerGlobalBean", Class.class, Object.class);
            registerMethod.invoke(null, TimeService.class, this);
            logInfo("[Time] TimeService registered as global bean");
        } catch (Exception e) {
            logWarning("[Time] Failed to register global bean: " + e.getMessage());
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

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
