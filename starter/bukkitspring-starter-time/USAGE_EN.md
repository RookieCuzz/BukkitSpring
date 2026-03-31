# Time Starter (Usage)

## Overview
The time starter provides a global `TimeService` for:
- Getting current time (`Instant`, millis, formatted string)
- Applying debug offset without changing OS time
- Optionally setting server system time (disabled by default)

`TimeService` is registered as a global bean only when `time.enabled=true`.

## Configuration
Edit `plugins/BukkitSpring/config.yml`.

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

Notes:
- `debug-offset-minutes` only affects this service and does not modify OS clock.
- `system-time.allow-set` must be explicitly enabled.
- Setting OS time requires administrator/root permissions.

## Usage
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
