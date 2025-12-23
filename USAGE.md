# BukkitSpring 使用文档

BukkitSpring 是一个轻量 IoC/DI 容器插件，供其他 Spigot/Paper 插件在运行时注册并自动注入依赖。

## 1. 安装与依赖

1) 将 BukkitSpring 构建后的 jar 放入服务器 `plugins/` 目录。
2) 你的业务插件在 `plugin.yml` 中声明依赖：

```yaml
name: YourPlugin
main: com.your.plugin.YourPlugin
version: 1.0.0
depend: [BukkitSpring]
```

## 2. 在你的插件中接入

在插件主类里注册并启动容器：

```java
import com.monstercontroller.bukkitspring.BukkitSpring;
import com.monstercontroller.bukkitspring.api.ApplicationContext;
import org.bukkit.plugin.java.JavaPlugin;

public final class YourPlugin extends JavaPlugin {
    private ApplicationContext context;

    @Override
    public void onLoad() {
        context = BukkitSpring.registerPlugin(this, "com.your.plugin");
    }

    @Override
    public void onEnable() {
        context.refresh();
    }

    @Override
    public void onDisable() {
        BukkitSpring.unregisterPlugin(this);
    }
}
```

- `registerPlugin(this, basePackages...)` 会扫描并注册 Bean。
- `refresh()` 会创建单例并执行 `@PostConstruct`。
- `unregisterPlugin(this)` 会执行 `@PreDestroy`。

## 3. 注解与基本用法

### 3.1 组件注册

```java
import com.monstercontroller.bukkitspring.api.annotation.Component;

@Component
public final class GreetingService {
    public String greet(String name) {
        return "Hello " + name;
    }
}
```

### 3.2 构造器注入（推荐）

```java
import com.monstercontroller.bukkitspring.api.annotation.Component;
import com.monstercontroller.bukkitspring.api.annotation.Autowired;

@Component
public final class Greeter {
    private final GreetingService service;

    @Autowired
    public Greeter(GreetingService service) {
        this.service = service;
    }
}
```

### 3.3 字段或方法注入（可选）

```java
import com.monstercontroller.bukkitspring.api.annotation.Autowired;
import com.monstercontroller.bukkitspring.api.annotation.Component;

@Component
public final class Greeter {
    @Autowired
    private GreetingService service;

    @Autowired
    public void setup(GreetingService service) {
        // setter/method injection
    }
}
```

### 3.4 配置类与 @Bean

```java
import com.monstercontroller.bukkitspring.api.annotation.Bean;
import com.monstercontroller.bukkitspring.api.annotation.Configuration;

@Configuration
public class MyConfig {
    @Bean
    public SomeService someService() {
        return new SomeService();
    }
}
```

### 3.5 作用域

```java
import com.monstercontroller.bukkitspring.api.annotation.Component;
import com.monstercontroller.bukkitspring.api.annotation.Scope;
import com.monstercontroller.bukkitspring.api.annotation.ScopeType;

@Component
@Scope(ScopeType.PROTOTYPE)
public class TempObject {
}
```

### 3.6 多实现冲突：@Primary / @Qualifier

```java
import com.monstercontroller.bukkitspring.api.annotation.Component;
import com.monstercontroller.bukkitspring.api.annotation.Primary;

@Component
@Primary
public class DefaultStorage implements Storage {
}
```

```java
import com.monstercontroller.bukkitspring.api.annotation.Autowired;
import com.monstercontroller.bukkitspring.api.annotation.Component;
import com.monstercontroller.bukkitspring.api.annotation.Qualifier;

@Component
public class StorageUser {
    @Autowired
    public StorageUser(@Qualifier("fastStorage") Storage storage) {
    }
}
```

### 3.7 延迟注入与 Provider

```java
import com.monstercontroller.bukkitspring.api.Provider;
import com.monstercontroller.bukkitspring.api.annotation.Autowired;
import com.monstercontroller.bukkitspring.api.annotation.Component;

@Component
public class LazyUser {
    private final Provider<HeavyService> provider;

    @Autowired
    public LazyUser(Provider<HeavyService> provider) {
        this.provider = provider;
    }

    public void use() {
        HeavyService service = provider.get();
    }
}
```

```java
import com.monstercontroller.bukkitspring.api.annotation.Lazy;
import com.monstercontroller.bukkitspring.api.annotation.Autowired;

@Component
public class LazyProxyUser {
    @Autowired
    public LazyProxyUser(@Lazy SomeInterface service) {
    }
}
```

### 3.8 生命周期

```java
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class LifecycleBean {
    @PostConstruct
    public void init() {
    }

    @PreDestroy
    public void close() {
    }
}
```

## 4. 内置可注入对象

容器启动时会自动提供：

- `JavaPlugin`
- `Plugin`
- `Logger`
- `Server`
- `PluginManager`
- `FileConfiguration`
- `BukkitScheduler`

## 5. 常见错误

- 无可用 bean：检查扫描包名是否正确，类是否加了 `@Component`。
- 多个候选：加 `@Primary` 或在注入点使用 `@Qualifier`。
- 循环依赖：改为 `Provider<T>` 或拆分依赖。

