# bukkitspring-starter-config Usage

## 1. What it provides

- unified config loading (local file + optional remote HTTP)
- load strategies:
  - `local-only`
  - `remote-only`
  - `local-first`
  - `remote-first`
- built-in cache (TTL + max entries)
- plugin-side parsing rules (`ConfigParser<T>`)

## 2. BukkitSpring config

Add to `plugins/BukkitSpring/config.yml`:

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

## 3. Plugin-side usage (only parsing logic)

### 3.1 Inject `ConfigService`

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

### 3.2 Load and parse

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

For local files, set `.plugin("YourPluginName")` so the starter reads from that plugin's data directory.

### 3.3 Override strategy per request

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

## 4. Access as global bean

```java
ConfigService service = com.cuzz.bukkitspring.BukkitSpring.getGlobalBean(ConfigService.class);
```
