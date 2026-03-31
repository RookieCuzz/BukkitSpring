# RocketMQ Starter 使用文档

## 概述
RocketMQ starter 提供 `RocketMqService`，用于：
- 创建 Producer 并发送消息
- 创建 Push Consumer
- 快速注册并启动并发消费监听

仅当 `rocketmq.enabled=true` 时才会注册全局 `RocketMqService`。

## 安装
1. 将 `bukkitspring-starter-rocketmq-*.jar` 放入 `plugins/BukkitSpring/starters/`。
2. 重启服务器。

## 配置
编辑 `plugins/BukkitSpring/config.yml`。

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

## 使用方式
### 注入
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

### 并发消费监听
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

### 全局 Bean 获取
```java
RocketMqService rocketMq = BukkitSpring.getGlobalBean(RocketMqService.class);
if (rocketMq != null && rocketMq.isEnabled()) {
  rocketMq.send("topic-demo", "from-global");
}
```

## 常见问题
- `RocketMqService missing`：starter 未加载或 `rocketmq.enabled=false`。
- `Failed to start RocketMQ producer`：检查 `rocketmq.namesrv-addr`。
- `Failed to start RocketMQ consumer`：检查 topic/group 配置及 broker ACL/网络。
