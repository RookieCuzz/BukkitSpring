# RocketMQ Starter (Usage)

## Overview
The RocketMQ starter provides a `RocketMqService` facade for:
- Producer creation and message sending
- Push consumer creation
- Quick concurrent subscription helper

`RocketMqService` is registered as a global bean only when `rocketmq.enabled=true`.

## Installation
1. Copy `bukkitspring-starter-rocketmq-*.jar` to `plugins/BukkitSpring/starters/`.
2. Restart the server.

## Configuration
Edit `plugins/BukkitSpring/config.yml`.

```yaml
rocketmq:
  enabled: true
  virtual-threads: true
  namesrv-addr: "127.0.0.1:9876"
  namespace: ""
  instance-name: "bukkitspring"
  access-channel: "LOCAL" # LOCAL | CLOUD
  use-tls: false
  vip-channel-enabled: false

  producer:
    group: "bukkitspring-producer"
    send-timeout-ms: 3000
    retry-times-when-send-failed: 2
    retry-times-when-send-async-failed: 2

  consumer:
    group: "bukkitspring-consumer"
    consume-from-where: "CONSUME_FROM_LAST_OFFSET"
    message-model: "CLUSTERING" # CLUSTERING | BROADCASTING
    consume-thread-min: 20
    consume-thread-max: 64
    max-reconsume-times: 16
    consume-timestamp: ""
```

## Usage
### Injection
```java
@Component
public final class RocketMqExample {
  @Autowired(required = false)
  private RocketMqService rocketMq;

  public void sendOnce() {
    if (rocketMq == null || !rocketMq.isEnabled()) {
      return;
    }
    rocketMq.send("topic-demo", "hello");
  }
}
```

### Subscribe with concurrent listener
```java
rocketMq.subscribeConcurrently(
    "demo-consumer-group",
    "topic-demo",
    "*",
    (messages, context) -> {
      for (MessageExt message : messages) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        System.out.println("RocketMQ: " + body);
      }
      return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    },
    Map.of("consume-thread-min", 8, "consume-thread-max", 16)
);
```

### Global bean lookup
```java
RocketMqService rocketMq = BukkitSpring.getGlobalBean(RocketMqService.class);
if (rocketMq != null && rocketMq.isEnabled()) {
  rocketMq.send("topic-demo", "from-global");
}
```

## Troubleshooting
- `RocketMqService missing`: starter jar not loaded or `rocketmq.enabled=false`.
- `Failed to start RocketMQ producer`: check `rocketmq.namesrv-addr`.
- `Failed to start RocketMQ consumer`: verify topic/group settings and broker ACL/network.
