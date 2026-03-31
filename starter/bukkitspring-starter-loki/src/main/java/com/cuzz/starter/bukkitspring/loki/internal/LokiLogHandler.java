package com.cuzz.starter.bukkitspring.loki.internal;

import com.cuzz.starter.bukkitspring.loki.config.LokiSettings;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public final class LokiLogHandler extends Handler {
    private static final class LokiLogEntry {
        private final LogRecord record;
        private final Map<String, String> labels;
        private final CompletableFuture<Boolean> ack;

        private LokiLogEntry(LogRecord record, Map<String, String> labels, CompletableFuture<Boolean> ack) {
            this.record = record;
            this.labels = labels == null || labels.isEmpty() ? null : new LinkedHashMap<>(labels);
            this.ack = ack;
        }
    }

    private final LokiSettings settings;
    private final BlockingQueue<LokiLogEntry> queue;
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
        enqueue(record, null);
    }

    public boolean enqueue(LogRecord record, Map<String, String> labels) {
        if (record == null || !isLoggable(record) || !running.get()) {
            return false;
        }
        return offerEntry(new LokiLogEntry(record, labels, null));
    }

    public CompletableFuture<Boolean> enqueueWithAck(LogRecord record, Map<String, String> labels) {
        CompletableFuture<Boolean> ack = new CompletableFuture<>();
        if (record == null || !isLoggable(record) || !running.get()) {
            ack.complete(false);
            return ack;
        }
        boolean offered = offerEntry(new LokiLogEntry(record, labels, ack));
        if (!offered) {
            ack.complete(false);
        }
        return ack;
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

    private boolean offerEntry(LokiLogEntry entry) {
        if (entry == null) {
            return false;
        }
        boolean offered = queue.offer(entry);
        if (offered) {
            return true;
        }
        if (settings.dropPolicy == LokiSettings.DropPolicy.DROP_OLDEST) {
            LokiLogEntry dropped = queue.poll();
            completeAck(dropped, false);
            return queue.offer(entry);
        }
        return false;
    }

    private void runWorker() {
        while (running.get() || !queue.isEmpty()) {
            List<LokiLogEntry> batch = new ArrayList<>(settings.maxBatchSize);
            LokiLogEntry first = pollEntry();
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
                    LokiLogEntry next = pollEntry(remaining, TimeUnit.NANOSECONDS);
                    if (next == null) {
                        break;
                    }
                    batch.add(next);
                }
            }
            sendBatch(batch);
        }
    }

    private LokiLogEntry pollEntry() {
        return pollEntry(settings.maxWaitMillis, TimeUnit.MILLISECONDS);
    }

    private LokiLogEntry pollEntry(long timeout, TimeUnit unit) {
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
        List<LokiLogEntry> batch = new ArrayList<>(settings.maxBatchSize);
        queue.drainTo(batch, settings.maxBatchSize);
        while (!batch.isEmpty()) {
            sendBatch(batch);
            batch.clear();
            queue.drainTo(batch, settings.maxBatchSize);
        }
    }

    private void sendBatch(List<LokiLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        String payload = buildPayload(entries);
        if (payload == null) {
            completeAckBatch(entries, false);
            return;
        }
        if (settings.debug) {
            debugLog("Loki push batch size=" + entries.size() + " endpoint=" + endpoint);
        }
        HttpRequest request = buildRequest(payload);
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (!isSuccessStatus(status)) {
                logFailureStatus(status);
                completeAckBatch(entries, false);
                return;
            }
            logSuccessStatus(status);
            completeAckBatch(entries, true);
        } catch (Exception ex) {
            logPushException(ex);
            completeAckBatch(entries, false);
        }
    }

    private HttpRequest buildRequest(String payload) {
        HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMillis(settings.requestTimeoutMillis))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));
        if (!settings.tenantId.isEmpty()) {
            request.header("X-Scope-OrgID", settings.tenantId);
        }
        return request.build();
    }

    private boolean isSuccessStatus(int status) {
        return status == 204 || status == 200;
    }

    private void logFailureStatus(int status) {
        throttleError("Loki push failed with status " + status);
    }

    private void logSuccessStatus(int status) {
        if (settings.debug) {
            debugLog("Loki push success status=" + status);
        }
    }

    private void logPushException(Exception ex) {
        throttleError("Loki push failed: " + ex.getMessage());
        if (settings.debug) {
            debugLog("Loki push exception: " + ex.getClass().getSimpleName());
        }
    }

    private static void completeAckBatch(List<LokiLogEntry> entries, boolean success) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (LokiLogEntry entry : entries) {
            completeAck(entry, success);
        }
    }

    private static void completeAck(LokiLogEntry entry, boolean success) {
        if (entry == null || entry.ack == null) {
            return;
        }
        entry.ack.complete(success);
    }

    private String buildPayload(List<LokiLogEntry> entries) {
        Map<String, List<LokiLogEntry>> grouped = groupEntries(entries);
        if (grouped.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder(512);
        appendStreams(builder, grouped);
        return builder.toString();
    }

    private Map<String, List<LokiLogEntry>> groupEntries(List<LokiLogEntry> entries) {
        Map<String, List<LokiLogEntry>> grouped = new LinkedHashMap<>();
        for (LokiLogEntry entry : entries) {
            String labelJson = buildLabelJson(entry);
            grouped.computeIfAbsent(labelJson, key -> new ArrayList<>()).add(entry);
        }
        return grouped;
    }

    private void appendStreams(StringBuilder builder, Map<String, List<LokiLogEntry>> grouped) {
        builder.append("{\"streams\":[");
        boolean firstStream = true;
        for (Map.Entry<String, List<LokiLogEntry>> entry : grouped.entrySet()) {
            if (!firstStream) {
                builder.append(",");
            }
            firstStream = false;
            appendStream(builder, entry.getKey(), entry.getValue());
        }
        builder.append("]}");
    }

    private void appendStream(StringBuilder builder, String labelJson, List<LokiLogEntry> entries) {
        builder.append("{\"stream\":{").append(labelJson).append("},\"values\":[");
        boolean firstValue = true;
        for (LokiLogEntry logEntry : entries) {
            if (!firstValue) {
                builder.append(",");
            }
            firstValue = false;
            appendValue(builder, logEntry);
        }
        builder.append("]}");
    }

    private void appendValue(StringBuilder builder, LokiLogEntry logEntry) {
        LogRecord record = logEntry.record;
        long nanos = record.getMillis() * 1_000_000L;
        String line = getFormatter().format(record);
        builder.append("[\"")
                .append(nanos)
                .append("\",\"")
                .append(escapeJson(line))
                .append("\"]");
    }

    private String buildLabelJson(LokiLogEntry entry) {
        LogRecord record = entry.record;
        Map<String, String> labels = new LinkedHashMap<>(settings.staticLabels);
        if (settings.includeLoggerLabel && record.getLoggerName() != null && !labels.containsKey("logger")) {
            labels.put("logger", record.getLoggerName());
        }
        if (settings.includeLevelLabel && record.getLevel() != null && !labels.containsKey("level")) {
            labels.put("level", record.getLevel().getName());
        }
        if (settings.includePluginLabel && record.getLoggerName() != null && !labels.containsKey("plugin")) {
            labels.put("plugin", record.getLoggerName());
        }
        if (settings.serverLabel != null && !settings.serverLabel.isBlank() && !labels.containsKey("server")) {
            labels.put("server", settings.serverLabel);
        }
        mergeLabels(labels, entry.labels);
        if (labels.isEmpty()) {
            labels.put("app", "bukkitspring");
        }
        StringBuilder builder = new StringBuilder(64);
        boolean first = true;
        for (Map.Entry<String, String> label : labels.entrySet()) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            builder.append("\"")
                    .append(escapeJson(label.getKey()))
                    .append("\":\"")
                    .append(escapeJson(label.getValue()))
                    .append("\"");
        }
        return builder.toString();
    }

    private static void mergeLabels(Map<String, String> target, Map<String, String> extra) {
        if (extra == null || extra.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : extra.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            target.put(key, value);
        }
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
