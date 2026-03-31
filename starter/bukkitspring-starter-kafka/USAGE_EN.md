# Kafka Starter (Usage)

## Overview
The Kafka starter provides a `KafkaService` for producing messages and managing
consumers. It supports custom client properties for producer/consumer/admin and
an integrated consumer manager (register/start/stop/pause/resume).

`KafkaService` is registered as a global bean only when `kafka.enabled=true`.

## Installation
1. Copy `bukkitspring-starter-kafka-*.jar` to `plugins/BukkitSpring/starters/`.
2. Restart the server.

## Configuration
Edit `plugins/BukkitSpring/config.yml`.

```yaml
kafka:
  enabled: true
  virtual-threads: true
  bootstrap-servers: "127.0.0.1:9092"
  client-id: "bukkitspring"

  # Base properties applied to producer/consumer/admin
  properties:
    acks: "all"

  # Producer-only overrides
  producer:
    linger.ms: 0
    batch.size: 16384

  # Consumer-only overrides
  consumer:
    auto.offset.reset: "earliest"

  # Admin client overrides
  admin:
    request.timeout.ms: 30000

  consumer-manager:
    shutdown-timeout: 30
    error-handling: "SKIP" # SKIP | RETRY | DLQ | STOP
    enable-consume-logging: false
```

Notes:
- `kafka.properties` are merged into all clients.
- `kafka.producer`, `kafka.consumer`, and `kafka.admin` are per-client overrides.

## Usage
### Injection
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

### Custom producer/consumer
```java
KafkaProducer<String, String> producer =
    kafka.createProducer(Map.of(
        "key.serializer", "org.apache.kafka.common.serialization.StringSerializer",
        "value.serializer", "org.apache.kafka.common.serialization.StringSerializer"
    ));
```

### Consumer registration
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

## Troubleshooting
- `KafkaService missing`: starter not loaded or `kafka.enabled=false`.
- `KafkaService disabled`: disabled by config or startup failure.
- Serialization errors: ensure correct `key.serializer` / `value.serializer`.
