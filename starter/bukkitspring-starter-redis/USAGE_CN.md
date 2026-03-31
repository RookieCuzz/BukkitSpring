# Redis Starter 使用文档

## 概述
Redis starter 提供 `RedisService` 门面，底层使用 Jedis `UnifiedJedis`。
内置字符串、哈希、列表、集合、有序集合、位图等常用操作，并提供异步执行器。

仅当 `redis.enabled=true` 时才会注册全局 `RedisService`。

## 安装
1. 将 `bukkitspring-starter-redis-*.jar` 放入 `plugins/BukkitSpring/starters/`。
2. 重启服务器。

## 配置
编辑 `plugins/BukkitSpring/config.yml`（Paper/Bukkit/Velocity 路径一致）。

```yaml
redis:
  enabled: true
  virtual-threads: true
  mode: standalone # standalone | cluster（当配置 nodes 时会自动切换为 cluster）
  host: localhost
  port: 6379
  user: ""         # 可选（ACL 用户名）
  password: ""     # 可选
  database: 0
  ssl: false
  timeouts:
    connect-ms: 2000
    socket-ms: 2000
  client-name: "bukkitspring"
  pool:
    max-total: 16
    max-idle: 16
    min-idle: 0
    max-wait-ms: 3000
  cluster:
    enabled: false
    nodes: ["127.0.0.1:6379", "127.0.0.1:6380"]
    max-redirects: 5
    topology-refresh-ms: 0
```

说明：
- 只要 `redis.cluster.enabled=true` 或 `redis.cluster.nodes` 不为空，都会强制启用 **cluster** 模式。
- Redis Cluster 不支持数据库选择，因此 cluster 模式下 `redis.database` 会被忽略。

## 使用方式
### 注入（推荐）
```java
@Component
public final class RedisExample {
  @Autowired(required = false)
  private RedisService redis;

  public void run() {
    if (redis == null || !redis.isEnabled()) {
      return;
    }
    redis.set("bstest:key", "value");
    String value = redis.get("bstest:key");
  }
}
```

### 全局 Bean 获取
```java
RedisService redis = BukkitSpring.getGlobalBean(RedisService.class);
if (redis != null && redis.isEnabled()) {
  redis.incr("bstest:counter");
}
```

### 异步执行器
```java
redis.runAsync(() -> redis.set("bstest:async", "1"));
```

### Pub/Sub（消息发布订阅）
```java
redis.publish("channel:test", "hello");

redis.subscribeAsync(new JedisPubSub() {
  @Override
  public void onMessage(String channel, String message) {
    System.out.println(channel + " -> " + message);
  }
}, "channel:test");
```

### Stream（消息流）
```java
redis.xadd("stream:test", Map.of("type", "join", "user", "alex"));
redis.xgroupCreate("stream:test", "group-a", new StreamEntryID("0-0"), true);

Map<String, StreamEntryID> streams = Map.of("stream:test", StreamEntryID.UNRECEIVED_ENTRY);
Map<String, List<StreamEntry>> records = redis.xreadGroupAsMap(
    "group-a",
    "consumer-1",
    XReadGroupParams.xReadGroupParams().count(10).block(2000),
    streams
);
```

说明：
- `subscribe/psubscribe` 是阻塞调用，建议使用 `subscribeAsync/psubscribeAsync`。
- `blpop/brpop` 已可用于阻塞队列消费。

## 常见问题
- `RedisService missing`：starter 未加载或 `redis.enabled=false`。
- `RedisService disabled`：服务未启用或已关闭。
- 认证失败：检查 `redis.user`、`redis.password`、`redis.ssl`。
