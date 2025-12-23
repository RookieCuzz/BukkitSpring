# BukkitSpring

BukkitSpring is a lightweight IoC/DI container plugin for Spigot/Paper. It scans your plugin classes, wires dependencies, and manages simple lifecycle callbacks.

If you only need usage notes, see `USAGE.md`.

## Features

- Annotation-driven component scanning
- Constructor injection by default
- Optional field/method injection
- `@PostConstruct` / `@PreDestroy` lifecycle hooks
- `@Primary` / `@Qualifier` for conflict resolution
- `Provider<T>` and `@Lazy` for deferred access
- Built-in bindings for common Bukkit objects

## Install

1) Put the BukkitSpring jar into the server `plugins/` directory.
2) In your plugin `plugin.yml`, declare a dependency:

```yaml
name: YourPlugin
main: com.your.plugin.YourPlugin
version: 1.0.0
depend: [BukkitSpring]
```

## Quick Start

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

## Annotations

### @Component

```java
import com.monstercontroller.bukkitspring.api.annotation.Component;

@Component
public final class GreetingService {
    public String greet(String name) {
        return "Hello " + name;
    }
}
```

### Constructor injection (recommended)

```java
import com.monstercontroller.bukkitspring.api.annotation.Autowired;
import com.monstercontroller.bukkitspring.api.annotation.Component;

@Component
public final class Greeter {
    private final GreetingService service;

    @Autowired
    public Greeter(GreetingService service) {
        this.service = service;
    }
}
```

### Field or method injection (optional)

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

### @Configuration and @Bean

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

### @Scope

```java
import com.monstercontroller.bukkitspring.api.annotation.Component;
import com.monstercontroller.bukkitspring.api.annotation.Scope;
import com.monstercontroller.bukkitspring.api.annotation.ScopeType;

@Component
@Scope(ScopeType.PROTOTYPE)
public class TempObject {
}
```

### @Primary and @Qualifier

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

### Provider and @Lazy

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
import com.monstercontroller.bukkitspring.api.annotation.Component;

@Component
public class LazyProxyUser {
    @Autowired
    public LazyProxyUser(@Lazy SomeInterface service) {
    }
}
```

### Lifecycle

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

## Built-in bindings

These are always available for injection:

- `JavaPlugin`
- `Plugin`
- `Logger`
- `Server`
- `PluginManager`
- `FileConfiguration`
- `BukkitScheduler`

## Example Plugin (with command)

A complete example lives under `examples/ExamplePlugin` and includes a `/hello` command wired through the container.

Structure:

```
examples/ExamplePlugin/
  build.gradle
  settings.gradle
  src/main/java/com/example/bukkitspringdemo/
    ExamplePlugin.java
    GreetingService.java
    GreetingCommand.java
    CommandRegistrar.java
  src/main/resources/
    plugin.yml
```

### Key classes

- `ExamplePlugin` registers and refreshes the container.
- `CommandRegistrar` uses `@PostConstruct` to register the command with an injected `JavaPlugin`.
- `GreetingCommand` is injected with `GreetingService` and implements `CommandExecutor`.

Build the framework plugin first so the example can compile against it:

```powershell
./gradlew shadowJar
```

Then build the example:

```powershell
cd examples/ExamplePlugin
./gradlew build
```

## Common errors

- Missing bean: check scan base package and `@Component`.
- Multiple candidates: add `@Primary` or `@Qualifier`.
- Circular dependency: refactor or use `Provider<T>`.

