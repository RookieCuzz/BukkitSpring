# Redis Starter (Usage)

## Overview
The Redis starter provides a `RedisService` facade backed by Jedis `UnifiedJedis`.
It exposes common Redis operations (string, hash, list, set, zset, bitmap) plus
an async executor.

`RedisService` is registered as a global bean only when `redis.enabled=true`.

## Installation
1. Copy `bukkitspring-starter-redis-*.jar` to `plugins/BukkitSpring/starters/`.
2. Restart the server.

## Configuration
Edit `plugins/BukkitSpring/config.yml` (Paper/Bukkit/Velocity use the same path).

```yaml
redis:
  enabled: true
  virtual-threads: true
  mode: standalone # standalone | cluster (auto switches to cluster when nodes are set)
  host: localhost
  port: 6379
  user: ""         # optional (ACL username)
  password: ""     # optional
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

Notes:
- If `redis.cluster.enabled=true` or `redis.cluster.nodes` is non-empty, the
  starter forces **cluster** mode regardless of `redis.mode`.
- Redis Cluster does not support database selection, so `redis.database` is
  ignored in cluster mode.

## Usage
### Injection (recommended)
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

### Global bean lookup
```java
RedisService redis = BukkitSpring.getGlobalBean(RedisService.class);
if (redis != null && redis.isEnabled()) {
  redis.incr("bstest:counter");
}
```

### Async executor
```java
redis.runAsync(() -> redis.set("bstest:async", "1"));
```

### Pub/Sub
```java
redis.publish("channel:test", "hello");

redis.subscribeAsync(new JedisPubSub() {
  @Override
  public void onMessage(String channel, String message) {
    System.out.println(channel + " -> " + message);
  }
}, "channel:test");
```

### Stream
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

Notes:
- `subscribe/psubscribe` are blocking calls; prefer `subscribeAsync/psubscribeAsync`.
- `blpop/brpop` are available for blocking queue consumption.

## Troubleshooting
- `RedisService missing`: starter jar not loaded or `redis.enabled=false`.
- `RedisService disabled`: `redis.enabled=false` or service has been closed.
- Authentication failures: check `redis.user`, `redis.password`, and `redis.ssl`.
