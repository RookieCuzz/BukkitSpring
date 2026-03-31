# BukkitSpring Starter 开发使用指南

> 面向需要扩展 BukkitSpring 功能的开发者，说明如何编写、打包并加载 Starter。

## 1. Starter 是什么

Starter 是一个**独立的 jar 模块**，被 BukkitSpring 在启动时加载，用于：

- 注册第三方依赖（运行时下载并加入 classpath）。
- 提供自动配置（`@Configuration` + `@Bean`）。
- 注册全局 Bean，供所有插件直接注入或获取。

Starter 本身**不是 Bukkit 插件**，不需要 `plugin.yml`。

## 2. 加载流程（关键时序）

1) BukkitSpring 在 `onLoad()` 扫描 `plugins/BukkitSpring/starters/` 目录。  
2) 对每个 starter jar，读取 `MANIFEST.MF` 中的 `Starter-Class` 并执行 `Class.forName(...)`。  
3) Starter 入口类的静态块调用 `StarterRegistry` 注册依赖与扫描包。  
4) BukkitSpring 下载依赖（来源为 Maven Central），并加入 classpath。  
5) BukkitSpring 在 `onEnable()` 创建自身容器，扫描 starter 的配置包并初始化。  
6) Starter 可以在配置类中注册全局 Bean，供其它插件注入使用。

## 3. Starter 开发步骤

### 3.1 新建模块与依赖

Starter 是第三方依赖的中间层，负责封装外部库并将服务注册进 BukkitSpring 容器。  
一般不需要引入 `bukkit/spigot-api`，仅依赖 `BukkitSpring`（`provided`）即可。

### 3.2 编写 Starter 入口类

入口类负责在**类加载时**注册依赖与扫描包：

```java
public final class MyStarter {
    static {
        StarterRegistry.registerDependencies(List.of(
            new MavenDependency("org.example", "example-core", "1.2.3"),
            new MavenDependency("org.example", "example-extra", "1.2.3")
        ));
        StarterRegistry.registerScanPackage("com.example.mystarter.autoconfigure");
        System.out.println("[MyStarter] Registered dependencies and scan package");
    }

    private MyStarter() {
    }
}
```

要点：
- 依赖列表必须**手动列出所有运行期 jar**（不处理传递依赖）。
- 日志建议用 `System.out`，因为此时 Bukkit Logger 可能尚未初始化。
- `registerScanPackage(...)` 只会扫描该包及其子包里的 `@Component/@Configuration` 类；如需更多包，需多次注册。

### 3.3 编写自动配置类

Starter 的核心逻辑一般放在 `@Configuration` 中：

```java
@Configuration
public class MyStarterAutoConfiguration {
    @Bean
    public MyService myService(JavaPlugin plugin, Logger logger) {
        return new MyService(plugin.getConfig(), logger);
    }
}
```

### 3.4 使用 `@Component` 组件

Starter 中也可以直接使用 `@Component` / `@Service` 等组件注解（若有），
但要确保对应包已通过 `StarterRegistry.registerScanPackage(...)` 注册扫描。

```java
@Component
public final class MyStarterService {
    // ...
}
```

注意：
- 这些组件会进入 **BukkitSpring 插件自身的容器**，不会自动进入第三方插件的容器。
- 若希望第三方插件可直接注入，请将服务注册为全局 Bean（见下节）。

### 3.5 注册全局 Bean（可选）

如需让其它插件直接注入此服务，可注册为全局 Bean：

```java
BukkitSpring.registerGlobalBean(MyService.class, service);
```

**注意**：全局 Bean 会在 ApplicationContext 创建时绑定。  
若你希望其它插件能注入到该 Bean，应确保注册发生在它们调用 `registerPlugin(...)` 之前。

## 4. 打包与 MANIFEST 配置

Starter 必须在 `MANIFEST.MF` 中指定入口类：

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-jar-plugin</artifactId>
  <version>3.4.1</version>
  <configuration>
    <archive>
      <manifestEntries>
        <Starter-Class>com.example.mystarter.MyStarter</Starter-Class>
      </manifestEntries>
    </archive>
  </configuration>
</plugin>
```

## 5. 安装与测试

1) 将打包后的 starter jar 放入：  
   `plugins/BukkitSpring/starters/`
2) 启动服务器，观察日志：  
   - `Loaded starter from xxx.jar`  
   - `Registered ...`
3) 在目标插件中注入/使用 Starter 提供的 Bean。

## 6. 常见问题

- **找不到依赖**：确认依赖在 Maven Central，且 `StarterRegistry` 注册了所有需要的 jar。  
- **自动配置未生效**：确认扫描包已注册，并且包内类加了 `@Configuration`。  
- **全局 Bean 无法注入**：检查注册时序，确保注册在依赖插件创建 context 前完成。

