# BukkitSpring Redisson Starter

Redisson starter for BukkitSpring.

## Features

- Lazy creation and lifecycle management of `RedissonClient`
- Global bean registration for `RedissonService`
- Single-server and cluster mode configuration
- Common distributed object shortcuts:
  - bucket, map, map-cache
  - set, list, queue, delayed-queue, blocking-queue
  - local-cached-map, rate-limiter, batch, script
  - bloom-filter, id-generator, transaction helper
  - distributed executor, remote-service, search
  - lock, read-write lock, semaphore, countdown-latch
  - topic, reliable-topic, stream

## Install

1. Build this module:

```bash
mvn -pl starter/bukkitspring-starter-redisson -am package
```

2. Put `bukkitspring-starter-redisson-1.0.0.jar` into:

`plugins/BukkitSpring/starters/`

3. Restart server and check logs for:

`[RedissonStarter] Registered Redisson dependencies and configuration package`

## Minimal Config

```yaml
redisson:
  enabled: true
  mode: "single"
  address: "redis://127.0.0.1:6379"
  username: ""
  password: ""
  database: 0
```

## Usage

Inject `RedissonService`:

```java
@Autowired(required = false)
private RedissonService redissonService;
```

Then use:

```java
RMap<String, String> map = redissonService.getMap("demo:map");
map.put("k", "v");

RDelayedQueue<String> delayed = redissonService.getDelayedQueue("demo:delay");
delayed.offer("task", 10, TimeUnit.SECONDS);

redissonService.withLock("demo:lock", () -> {
    // critical section
});

boolean firstSeen = redissonService.getBloomFilter("demo:bloom").contains("player:1");
long seq = redissonService.getIdGenerator("demo:id").nextId();
redissonService.getExecutorService("demo:executor");
redissonService.getRemoteService("demo:rpc");
redissonService.getSearch();
redissonService.withTransaction(tx -> {
    tx.getMap("demo:tx-map").put("seq", seq);
    return null;
});
```

See also:

- `USAGE_EN.md`
- `USAGE_CN.md`
