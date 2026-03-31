# Redisson Starter 使用说明 (CN)

## 配置示例

```yaml
redisson:
  enabled: true
  virtual-threads: true
  mode: "single" # single | cluster
  address: "redis://127.0.0.1:6379"
  username: ""
  password: ""
  database: 0
  client-name: "bukkitspring-redisson"
  timeout-ms: 3000
  connect-timeout-ms: 10000
  idle-connection-timeout-ms: 10000
  retry-attempts: 3
  retry-interval-ms: 1500
  connection-pool-size: 64
  connection-minimum-idle-size: 24
  subscription-connection-pool-size: 50
  subscription-connection-minimum-idle-size: 1
  threads: 0
  netty-threads: 0
  transport-mode: "NIO" # NIO | EPOLL | KQUEUE
  cluster:
    enabled: false
    nodes: []
    scan-interval-ms: 1000
```

当 `cluster.enabled=true` 或者 `cluster.nodes` 非空时，模式会自动切换为 `cluster`。

## 注入服务

```java
import com.cuzz.starter.bukkitspring.redisson.api.RedissonService;

@Autowired(required = false)
private RedissonService redissonService;
```

## 常用 API

```java
redissonService.getBucket("demo:bucket").set("hello");
redissonService.getMap("demo:map").put("a", "1");
redissonService.getLock("demo:lock").lock();
redissonService.getTopic("demo:topic").publish("msg");
redissonService.getDelayedQueue("demo:delay").offer("payload", 5, java.util.concurrent.TimeUnit.SECONDS);
redissonService.getRateLimiter("demo:rl");
redissonService.getLocalCachedMap("demo:lcache");
redissonService.createBatch();
redissonService.getExecutorService("demo:executor");
redissonService.getScript();
redissonService.getIdGenerator("demo:id").nextId();
redissonService.getBloomFilter("demo:bloom");
redissonService.getRemoteService("demo:remote");
redissonService.getSearch();
redissonService.createTransaction();
redissonService.withLock("demo:lock", () -> { });
redissonService.withTransaction(tx -> {
    tx.getMap("demo:tx-map").put("k", "v");
    return null;
});
```

## 异步助手

```java
redissonService.runAsync(() -> {
    redissonService.getAtomicLong("demo:counter").incrementAndGet();
});
```
