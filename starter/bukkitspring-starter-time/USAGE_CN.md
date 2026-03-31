# Time Starter 使用文档

## 概述
time starter 提供全局 `TimeService`，用于：
- 获取当前时间（`Instant`、毫秒、格式化字符串）
- 应用调试时间偏移（不改系统时钟）
- 可选设置服务器系统时间（默认关闭）

仅当 `time.enabled=true` 时才会注册全局 `TimeService`。

## 配置
编辑 `plugins/BukkitSpring/config.yml`。

```yaml
time:
  enabled: true
  virtual-threads: true
  zone-id: "Asia/Shanghai"
  debug-offset-minutes: 0
  system-time:
    allow-set: false
    command-timeout-ms: 5000
    prefer-timedatectl: true
```

说明：
- `debug-offset-minutes` 只影响 `TimeService` 的时间，不修改操作系统时间。
- 必须显式开启 `system-time.allow-set=true` 才允许设置系统时间。
- 设置系统时间需要管理员/root 权限。

## 使用示例
```java
@Component
public final class TimeExample {
  @Autowired(required = false)
  private TimeService timeService;

  public void run() {
    if (timeService == null || !timeService.isEnabled()) {
      return;
    }
    long now = timeService.currentTimeMillis();
    String text = timeService.formatNow("yyyy-MM-dd HH:mm:ss", "UTC");
    TimeSetResult result = timeService.setSystemTime(Instant.now().plusSeconds(60));
  }
}
```
