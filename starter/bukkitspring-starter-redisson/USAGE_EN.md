# Redisson Starter Usage (EN)

## Config

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

When `cluster.enabled=true` or `cluster.nodes` is not empty, mode is forced to `cluster`.

## Inject Service

```java
import com.cuzz.starter.bukkitspring.redisson.api.RedissonService;

@Autowired(required = false)
private RedissonService redissonService;
```

## Common APIs

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

## Async Helpers

```java
redissonService.runAsync(() -> {
    redissonService.getAtomicLong("demo:counter").incrementAndGet();
});
```
