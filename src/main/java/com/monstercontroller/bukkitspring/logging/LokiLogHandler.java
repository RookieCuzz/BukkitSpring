package com.monstercontroller.bukkitspring.logging;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public final class LokiLogHandler extends Handler {
    private final LokiSettings settings;
    private final BlockingQueue<LogRecord> queue;
    private final HttpClient httpClient;
    private final URI endpoint;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread worker;
    private volatile long lastErrorLogMillis = 0L;
    private volatile long lastDebugLogMillis = 0L;

    public LokiLogHandler(LokiSettings settings) {
        this.settings = settings;
        this.queue = new LinkedBlockingQueue<>(settings.queueCapacity);
        this.endpoint = URI.create(settings.url);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(settings.connectTimeoutMillis))
                .build();
        setLevel(Level.ALL);
        setFormatter(new SimpleFormatter());
        this.worker = new Thread(this::runWorker, "bukkitspring-loki");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void publish(LogRecord record) {
        if (record == null || !isLoggable(record)) {
            return;
        }
        boolean offered = queue.offer(record);
        if (offered) {
            return;
        }
        if (settings.dropPolicy == LokiSettings.DropPolicy.DROP_OLDEST) {
            queue.poll();
            queue.offer(record);
        }
    }

    @Override
    public void flush() {
        drainAndSend();
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        worker.interrupt();
        try {
            worker.join(2000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        drainAndSend();
    }

    private void runWorker() {
        while (running.get() || !queue.isEmpty()) {
            List<LogRecord> batch = new ArrayList<>(settings.maxBatchSize);
            LogRecord first = pollRecord();
            if (first == null) {
                continue;
            }
            batch.add(first);
            if (settings.maxBatchSize > 1) {
                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(settings.maxWaitMillis);
                while (batch.size() < settings.maxBatchSize) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) {
                        break;
                    }
                    LogRecord next = pollRecord(remaining, TimeUnit.NANOSECONDS);
                    if (next == null) {
                        break;
                    }
                    batch.add(next);
                }
            }
            sendBatch(batch);
        }
    }

    private LogRecord pollRecord() {
        return pollRecord(settings.maxWaitMillis, TimeUnit.MILLISECONDS);
    }

    private LogRecord pollRecord(long timeout, TimeUnit unit) {
        try {
            if (timeout <= 0) {
                return queue.poll();
            }
            return queue.poll(timeout, unit);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private void drainAndSend() {
        List<LogRecord> batch = new ArrayList<>(settings.maxBatchSize);
        queue.drainTo(batch, settings.maxBatchSize);
        while (!batch.isEmpty()) {
            sendBatch(batch);
            batch.clear();
            queue.drainTo(batch, settings.maxBatchSize);
        }
    }

    private void sendBatch(List<LogRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        String payload = buildPayload(records);
        if (payload == null) {
            return;
        }
        if (settings.debug) {
            debugLog("Loki push batch size=" + records.size() + " endpoint=" + endpoint);
        }
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMillis(settings.requestTimeoutMillis))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));
        if (!settings.tenantId.isEmpty()) {
            request.header("X-Scope-OrgID", settings.tenantId);
        }
        try {
            HttpResponse<Void> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status != 204 && status != 200) {
                throttleError("Loki push failed with status " + status);
            } else if (settings.debug) {
                debugLog("Loki push success status=" + status);
            }
        } catch (Exception ex) {
            throttleError("Loki push failed: " + ex.getMessage());
            if (settings.debug) {
                debugLog("Loki push exception: " + ex.getClass().getSimpleName());
            }
        }
    }

    private String buildPayload(List<LogRecord> records) {
        Map<String, List<LogRecord>> grouped = new LinkedHashMap<>();
        for (LogRecord record : records) {
            String labelJson = buildLabelJson(record);
            grouped.computeIfAbsent(labelJson, key -> new ArrayList<>()).add(record);
        }
        if (grouped.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder(512);
        builder.append("{\"streams\":[");
        boolean firstStream = true;
        for (Map.Entry<String, List<LogRecord>> entry : grouped.entrySet()) {
            if (!firstStream) {
                builder.append(",");
            }
            firstStream = false;
            builder.append("{\"stream\":{").append(entry.getKey()).append("},\"values\":[");
            boolean firstValue = true;
            for (LogRecord record : entry.getValue()) {
                if (!firstValue) {
                    builder.append(",");
                }
                firstValue = false;
                long nanos = record.getMillis() * 1_000_000L;
                String line = getFormatter().format(record);
                builder.append("[\"")
                        .append(nanos)
                        .append("\",\"")
                        .append(escapeJson(line))
                        .append("\"]");
            }
            builder.append("]}");
        }
        builder.append("]}");
        return builder.toString();
    }

    private String buildLabelJson(LogRecord record) {
        Map<String, String> labels = new LinkedHashMap<>(settings.staticLabels);
        if (settings.includeLoggerLabel && record.getLoggerName() != null) {
            labels.put("logger", record.getLoggerName());
        }
        if (settings.includeLevelLabel && record.getLevel() != null) {
            labels.put("level", record.getLevel().getName());
        }
        if (settings.includePluginLabel && record.getLoggerName() != null) {
            labels.put("plugin", record.getLoggerName());
        }
        if (settings.serverLabel != null && !settings.serverLabel.isBlank()) {
            labels.put("server", settings.serverLabel);
        }
        if (labels.isEmpty()) {
            labels.put("app", "bukkitspring");
        }
        StringBuilder builder = new StringBuilder(64);
        boolean first = true;
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            builder.append("\"")
                    .append(escapeJson(entry.getKey()))
                    .append("\":\"")
                    .append(escapeJson(entry.getValue()))
                    .append("\"");
        }
        return builder.toString();
    }

    private void throttleError(String message) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLogMillis < 30000L) {
            return;
        }
        lastErrorLogMillis = now;
        System.err.println("[BukkitSpring] " + message);
    }

    private void debugLog(String message) {
        long now = System.currentTimeMillis();
        if (now - lastDebugLogMillis < 1000L) {
            return;
        }
        lastDebugLogMillis = now;
        System.out.println("[BukkitSpring] " + message);
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
            }
        }
        return out.toString();
    }
}
