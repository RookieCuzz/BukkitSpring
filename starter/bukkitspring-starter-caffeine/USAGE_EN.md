# Caffeine Starter (Usage)

## Overview
The caffeine starter provides a global `CaffeineService` for:
- Creating and accessing named caches
- Running common cache operations (get/put/invalidate)
- Wrapping Caffeine native interfaces (`Cache`, `AsyncCache`, `LoadingCache`, `AsyncLoadingCache`, `Policy`)
- Providing `typed*` helpers (generic views) to reduce `Object` casts

`CaffeineService` is registered as a global bean only when `caffeine.enabled=true`.

## Configuration
Edit `plugins/BukkitSpring/config.yml`.

```yaml
caffeine:
  enabled: true
  virtual-threads: true
  default-cache-name: "default"
  default:
    initial-capacity: 64
    maximum-size: 10000
    expire-after-write-ms: 0
    expire-after-access-ms: 0
    refresh-after-write-ms: 0
    weak-keys: false
    weak-values: false
    soft-values: false
    record-stats: false
  caches:
    player-profile:
      maximum-size: 20000
      expire-after-write-ms: 600000
```

Notes:
- `maximum-size=0` means no maximum size limit.
- `expire-after-*-ms=0` means no expiration.
- `refresh-after-write-ms=0` means no refresh policy.
- If both `weak-values` and `soft-values` are true, `weak-values` wins.

## Usage
```java
@Component
public final class CacheExample {
  @Autowired(required = false)
  private CaffeineService caffeineService;

  public String loadPlayerName(String playerId) {
    if (caffeineService == null || !caffeineService.isEnabled()) {
      return "unknown";
    }
    Object value = caffeineService.get("player-profile", playerId, key -> queryPlayerName((String) key));
    return value == null ? "unknown" : value.toString();
  }

  private String queryPlayerName(String playerId) {
    return "player-" + playerId;
  }
}
```

### Native Wrapper Example
```java
Cache<Object, Object> sync = caffeineService.getCache("player-profile");
AsyncCache<Object, Object> async = caffeineService.getAsyncCache("player-profile");

LoadingCache<Object, Object> loading = caffeineService.getLoadingCache(
    "player-loading",
    key -> "computed-" + key
);

Optional<Policy.Eviction<Object, Object>> eviction = caffeineService.evictionPolicy("player-profile");
```

### Typed API Example
```java
Integer value = caffeineService.typedGet("counter", "user:1", key -> 1);
caffeineService.typedPut("counter", "user:1", value + 1);
Integer latest = caffeineService.typedGetIfPresent("counter", "user:1");
Integer loaded = caffeineService.typedLoadingGet("counter-loading", "user:1", key -> 1);
```


