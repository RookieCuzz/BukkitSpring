# Loki Starter (Usage)

## Overview
The Loki starter ships logs to Grafana Loki using a JUL `Handler`.
It attaches `LokiLogHandler` to the server logger and/or the root logger via
the platform `LogHandlerBinder` SPI.

It also registers a `LokiLogService` global bean for direct pushes.

## Installation
1. Copy `bukkitspring-starter-loki-*.jar` to `plugins/BukkitSpring/starters/`.
2. Restart the server.

## Configuration
Edit `plugins/BukkitSpring/config.yml`.

```yaml
loki:
  enabled: true
  url: "http://127.0.0.1:3100/loki/api/v1/push"
  tenant-id: ""
  server-logger: true
  root-logger: false
  batch:
    max-size: 200
    max-wait-ms: 1000
  queue:
    capacity: 10000
    drop-policy: "drop-newest" # drop-newest | drop-oldest
  timeouts:
    connect-ms: 2000
    request-ms: 5000
  labels:
    include-logger: true
    include-level: true
    include-plugin: true
    server: "paper-1"
    static:
      env: "prod"
  debug: false
```

## Usage
### Automatic JUL capture
When enabled, logs written to the server logger or root logger are forwarded
to Loki (depending on `server-logger` / `root-logger`).

### Direct push (LokiLogService)
```java
@Component
public final class LokiExample {
  @Autowired(required = false)
  private LokiLogService loki;

  public void logOnce() {
    if (loki == null || !loki.isEnabled()) {
      return;
    }
    loki.log(Level.INFO, "hello loki", Map.of("event", "demo"));
  }
}
```

### Ack support
```java
CompletableFuture<Boolean> ack =
    loki.logWithAck(Level.INFO, "hello", Map.of("event", "demo"));
```

## Troubleshooting
- `Loki is enabled but loki.url is empty`: set `loki.url`.
- `No LogHandlerBinder found`: platform binder missing (ensure correct platform jar).
