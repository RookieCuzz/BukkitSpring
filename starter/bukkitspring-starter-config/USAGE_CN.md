# bukkitspring-starter-config 使用说明

## 1. 功能

- 统一配置读取入口（本地文件 + 预留远程 HTTP）
- 可配置读取策略：
  - `local-only`
  - `remote-only`
  - `local-first`
  - `remote-first`
- 内置缓存（TTL + 最大条目）
- 插件只写“解析规则”，读取逻辑由 starter 处理

## 2. BukkitSpring 配置

在 `plugins/BukkitSpring/config.yml` 中添加：

```yaml
config-starter:
  enabled: true
  virtual-threads: true
  cache:
    enabled: true
    ttl-ms: 5000
    max-entries: 256
  source:
    strategy: "local-first"
    local:
      enabled: true
      bootstrap-from-resource: true
      encoding: "UTF-8"
    remote:
      enabled: false
      provider: "http"
      base-url: ""
      path-template: "{name}"
      encoding: "UTF-8"
      connect-timeout-ms: 2000
      read-timeout-ms: 3000
      fail-on-http-error: false
      headers: {}
```

## 3. 插件侧使用（只写解析规则）

### 3.1 依赖注入

```java
import com.cuzz.starter.bukkitspring.config.api.ConfigService;

@Component
public final class MyConfigFacade {
    private final ConfigService configService;

    @Autowired
    public MyConfigFacade(@Autowired(required = false) ConfigService configService) {
        this.configService = configService;
    }
}
```

### 3.2 读取并解析

```java
import com.cuzz.starter.bukkitspring.config.api.ConfigQuery;
import org.bukkit.configuration.file.YamlConfiguration;

YamlConfiguration yaml = configService.load(
        ConfigQuery.builder("menu_icons.yml")
                .plugin("YourPluginName")
                .build(),
        doc -> YamlConfiguration.loadConfiguration(new java.io.StringReader(doc.content()))
);
```

读取本地文件时请设置 `.plugin("YourPluginName")`，这样 starter 才会从该插件自己的数据目录读取。

### 3.3 覆盖策略

```java
import com.cuzz.starter.bukkitspring.config.api.ConfigLoadStrategy;
import com.cuzz.starter.bukkitspring.config.api.ConfigQuery;

String text = configService.load(
        ConfigQuery.builder("rules.yml")
                .strategy(ConfigLoadStrategy.REMOTE_FIRST)
                .bypassCache(true)
                .build()
).content();
```

## 4. 全局 Bean 获取

```java
ConfigService service = com.cuzz.bukkitspring.BukkitSpring.getGlobalBean(ConfigService.class);
```
