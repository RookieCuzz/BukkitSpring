# Loki Starter 使用文档

## 概述
Loki starter 通过 JUL `Handler` 将日志推送到 Grafana Loki。
它会借助平台 `LogHandlerBinder` SPI，把 `LokiLogHandler` 挂载到
服务器日志器和/或 root logger。

同时提供 `LokiLogService` 全局 Bean 便于主动发送日志。

## 安装
1. 将 `bukkitspring-starter-loki-*.jar` 放入 `plugins/BukkitSpring/starters/`。
2. 重启服务器。

## 配置
编辑 `plugins/BukkitSpring/config.yml`。

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

## 使用方式
### 自动捕获 JUL 日志
启用后，服务器日志或 root logger 的日志会自动发送到 Loki
（取决于 `server-logger` / `root-logger`）。

### 主动发送（LokiLogService）
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

### Ack 判断
```java
CompletableFuture<Boolean> ack =
    loki.logWithAck(Level.INFO, "hello", Map.of("event", "demo"));
```

## 常见问题
- `Loki is enabled but loki.url is empty`：请设置 `loki.url`。
- `No LogHandlerBinder found`：平台 binder 未加载，请确认使用了正确的平台 jar。
