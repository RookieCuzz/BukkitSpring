# Caffeine Starter 使用文档

## 概述
`caffeine starter` 会提供全局 `CaffeineService`，目标是尽量贴近 Caffeine 3.1.8 原生接口，覆盖：
- `Cache`
- `AsyncCache`
- `LoadingCache`
- `AsyncLoadingCache`
- `Policy`

仅当 `caffeine.enabled=true` 时，才会注册全局 `CaffeineService`。

## 配置
编辑 `plugins/BukkitSpring/config.yml`。

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
      refresh-after-write-ms: 300000
```

说明：
- `maximum-size=0`：不限制最大条目数。
- `expire-after-*-ms=0`：不启用过期策略。
- `refresh-after-write-ms=0`：不启用刷新策略。
- `weak-values` 与 `soft-values` 同时为 `true` 时，优先 `weak-values`。

## API 对照

### 1) 构建与获取缓存
- `newBuilder(cacheName)`：返回带 starter 默认配置的 `Caffeine` builder，可继续叠加原生配置。
- `getCache(cacheName)`：获取 `Cache<Object, Object>`。
- `getAsyncCache(cacheName)`：获取 `AsyncCache<Object, Object>`。
- `getLoadingCache(cacheName, loader)`：获取 `LoadingCache<Object, Object>`。
- `getAsyncLoadingCache(cacheName, loader)`：获取 `AsyncLoadingCache<Object, Object>`。
- `resolveSpec(cacheName)`：获取当前缓存最终生效的配置。

### 2) Cache 常用操作
- 查询：`getIfPresent`、`get`、`getAllPresent`、`getAll`
- 写入：`put`、`putAll`
- 失效：`invalidate`、`invalidateAll(keys)`、`invalidateAll()`
- 维护：`estimatedSize`、`asMap`、`cleanUp`、`stats`

### 3) AsyncCache 操作
- 读取：`asyncGetIfPresent`、`asyncGet`
- 批量：`asyncGetAll`
- 写入：`asyncPut`
- 视图：`asyncAsMap`、`synchronous`

### 4) Loading/AsyncLoading 操作
- `loadingGet`、`loadingGetAll`
- `loadingRefresh`、`loadingRefreshAll`
- `asyncLoadingGet`、`asyncLoadingGetAll`
- `asyncLoadingSynchronous`

### 5) Policy 操作
- 基础：`policy`、`isRecordingStats`、`refreshes`
- 淘汰：`evictionPolicy`
- 过期：`expireAfterAccessPolicy`、`expireAfterWritePolicy`、`variableExpirationPolicy`
- 刷新：`refreshAfterWritePolicy`
- 安静读取：`getIfPresentQuietly`、`getEntryIfPresentQuietly`

### 6) 生命周期与管理
- `cacheNames`：查看已创建缓存名称。
- `destroyCache(name)`：销毁单个缓存。
- `destroyAllCaches()`：销毁全部缓存。
- `close()`：关闭服务并释放资源。

### 7) 强类型 Typed API
- `typedBuilder`、`typedCache`、`typedAsyncCache`
- `typedLoadingCache`、`typedAsyncLoadingCache`
- `typedPolicy`
- `typedGetIfPresent`、`typedGet`、`typedPut`
- `typedAsyncGet`、`typedLoadingGet`、`typedAsyncLoadingGet`

## 使用示例

### 1) 直接用原生 Cache
```java
@Component
public final class PlayerCacheFacade {
  @Autowired(required = false)
  private CaffeineService caffeineService;

  public String getPlayerName(String playerId) {
    if (caffeineService == null || !caffeineService.isEnabled()) {
      return "unknown";
    }
    Object value = caffeineService.get(
        "player-profile",
        playerId,
        key -> loadNameFromDb((String) key)
    );
    return value == null ? "unknown" : value.toString();
  }

  private String loadNameFromDb(String playerId) {
    return "player-" + playerId;
  }
}
```

### 2) LoadingCache + Refresh
```java
CacheLoader<Object, Object> loader = key -> queryFromRemote((String) key);
Object value = caffeineService.loadingGet("player-loading", "u1001", loader);

CompletableFuture<Object> refreshFuture =
    caffeineService.loadingRefresh("player-loading", "u1001", loader);
```

### 3) 访问 Policy
```java
Optional<Policy.Eviction<Object, Object>> eviction =
    caffeineService.evictionPolicy("player-profile");

if (eviction.isPresent()) {
  long max = eviction.get().getMaximum();
}
```

### 4) 强类型写法（减少 Object 强转）
```java
Integer value = caffeineService.typedGet("counter", "user:1", key -> 1);
caffeineService.typedPut("counter", "user:1", value + 1);
Integer latest = caffeineService.typedGetIfPresent("counter", "user:1");
Integer loaded = caffeineService.typedLoadingGet("counter-loading", "user:1", key -> 1);
```

## 设计说明
- 该 starter 是“包装层 + 默认配置层”，不是重新实现缓存算法。
- 复杂场景建议先 `newBuilder(cacheName)` 拿到 builder，再按业务追加原生能力。
- Object 风格 API 保留是为了兼容；新代码建议优先使用 `typed*` 系列方法。
