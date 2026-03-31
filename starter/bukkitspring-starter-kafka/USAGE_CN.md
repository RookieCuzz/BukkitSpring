# Kafka Starter 使用文档

## 概述
Kafka starter 提供 `KafkaService`，用于消息发送与消费者管理（注册/启动/暂停/恢复/停止）。
支持 producer/consumer/admin 的独立配置，并提供统一的异步执行器。

仅当 `kafka.enabled=true` 时才会注册全局 `KafkaService`。

## 安装
1. 将 `bukkitspring-starter-kafka-*.jar` 放入 `plugins/BukkitSpring/starters/`。
2. 重启服务器。

## 配置
编辑 `plugins/BukkitSpring/config.yml`。

```yaml
kafka:
  enabled: true
  virtual-threads: true
  bootstrap-servers: "127.0.0.1:9092"
  client-id: "bukkitspring"

  # 作用于 producer/consumer/admin 的基础配置
  properties:
    acks: "all"

  # Producer 覆盖配置
  producer:
    linger.ms: 0
    batch.size: 16384

  # Consumer 覆盖配置
  consumer:
    auto.offset.reset: "earliest"

  # Admin 覆盖配置
  admin:
    request.timeout.ms: 30000

  consumer-manager:
    shutdown-timeout: 30
    error-handling: "SKIP" # SKIP | RETRY | DLQ | STOP
    enable-consume-logging: false
```

说明：
- `kafka.properties` 会合并到所有客户端。
- `kafka.producer` / `kafka.consumer` / `kafka.admin` 为独立覆盖配置。

## 使用方式
### 注入
```java
@Component
public final class KafkaExample {
  @Autowired(required = false)
  private KafkaService kafka;

  public void sendOnce() {
    if (kafka == null || !kafka.isEnabled()) {
      return;
    }
    kafka.send("topic-demo", "hello");
  }
}
```

### 自定义 Producer/Consumer
```java
KafkaProducer<String, String> producer =
    kafka.createProducer(Map.of(
        "key.serializer", "org.apache.kafka.common.serialization.StringSerializer",
        "value.serializer", "org.apache.kafka.common.serialization.StringSerializer"
    ));
```

### 消费者注册
```java
ConsumerRegistration<String, String> registration =
    ConsumerRegistration.<String, String>builder()
        .topics("topic-demo")
        .groupId("demo-group")
        .handler(record -> {
          System.out.println("value=" + record.value());
        })
        .autoStartup(true)
        .build();

String consumerId = kafka.registerConsumer(registration);
kafka.startConsumer(consumerId);
```

## 常见问题
- `KafkaService missing`：starter 未加载或 `kafka.enabled=false`。
- `KafkaService disabled`：配置关闭或启动失败。
- 序列化错误：检查 `key.serializer` / `value.serializer`。
