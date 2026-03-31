# 外部依赖下载方式与迁移说明（精简版）

## 概述

本项目采用“运行期下载 + 运行期动态加载”的方式获取外部库。启动时从 Maven Central 下载指定版本的 JAR，并通过反射向类加载器追加 jar 路径。

## 关键代码（示意）

```java
// 1) 组装 Maven Central URL
String url = base + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/"
    + artifactId + "-" + version + ".jar";

// 2) 下载到目标目录
try (InputStream in = new URL(url).openStream()) {
    Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
}

// 3) 动态加入类加载器
addJarToClasspath(targetFile);
```

## 启动流程（关键步骤）

1. 插件启动钩子中异步触发下载。
2. 缺失的 jar 从 Maven Central 拉取到 `<serverRoot>/libraries`。
3. 下载完成后将 jar 追加到运行时类加载器。
4. 失败则禁用插件，成功继续初始化。

## 迁移到其他插件

1. 抽出“下载 + 动态加载”的工具逻辑到你的项目。
2. 在启动钩子中异步下载，成功后回主线程初始化。
3. 根据需求维护依赖清单（`groupId/artifactId/version`）。
4. 目录可用 `<serverRoot>/libraries`（共享缓存）或 `<pluginData>/libraries`（隔离）。

## 注意事项

- 不处理传递依赖，需要手动补齐。
- 若运行时类加载器不支持追加 jar，需要替代加载方案。
- 无校验和与重试策略，生产环境建议补充。

## 黑科技方式与 JDK 限制

运行期动态加载依赖通常依赖以下“黑科技”思路之一：

1. 反射调用 `URLClassLoader#addURL`
2. 通过 `Unsafe` 直接修改 `URLClassLoader` 内部的 `ucp` 路径集合

这些方式在 JDK 9+ 的模块系统下可能被限制，常见报错是：

- `InaccessibleObjectException: ... java.base does not "opens java.net" to unnamed module`

这类问题的应对方式：

- 启动参数开放模块：`--add-opens java.base/java.net=ALL-UNNAMED`
- 或避免反射/Unsafe，改用可扩展的类加载器实现（例如自定义 `URLClassLoader`）

如果你的插件运行在 Paper/Spigot 的新版本上，建议优先选择“可控的类加载器”方案，避免模块访问限制导致插件启动失败。

## 详细代码（可直接复用）

### 1) 下载 + 动态加载

```java
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class DependencyDownloader {
    private static final String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2/";

    public static boolean downloadAndLoad(String groupId, String artifactId, String version, File targetDir) {
        String fileName = artifactId + "-" + version + ".jar";
        File jarFile = new File(targetDir, fileName);

        if (!jarFile.exists() && !downloadJar(groupId, artifactId, version, jarFile)) {
            return false;
        }

        try {
            addJarToClasspath(jarFile);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean downloadJar(String groupId, String artifactId, String version, File jarFile) {
        String groupPath = groupId.replace(".", "/");
        String url = MAVEN_CENTRAL_URL + groupPath + "/" + artifactId + "/" + version + "/"
            + artifactId + "-" + version + ".jar";

        try (InputStream in = new URL(url).openStream()) {
            Files.createDirectories(jarFile.getParentFile().toPath());
            Files.copy(in, jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static void addJarToClasspath(File jarFile) throws Exception {
        ClassLoader cl = DependencyDownloader.class.getClassLoader();
        if (!(cl instanceof URLClassLoader urlCl)) {
            throw new IllegalStateException("ClassLoader is not URLClassLoader.");
        }

        URL jarUrl = jarFile.toURI().toURL();
        try {
            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
            addUrl.invoke(urlCl, jarUrl);
        } catch (Throwable reflectFailed) {
            URLClassLoaderAccess access = URLClassLoaderAccess.create(urlCl);
            access.addURL(jarUrl);
        }
    }
}
```

### 2) URLClassLoaderAccess（Unsafe 追加路径）

```java
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

public abstract class URLClassLoaderAccess {
    private final URLClassLoader classLoader;

    static URLClassLoaderAccess create(URLClassLoader classLoader) {
        if (UnsafeAccess.isSupported()) {
            return new UnsafeAccess(classLoader);
        }
        return Noop.INSTANCE;
    }

    protected URLClassLoaderAccess(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public abstract void addURL(URL url);

    private static class UnsafeAccess extends URLClassLoaderAccess {
        private static final sun.misc.Unsafe UNSAFE;
        private final Collection<URL> unopenedURLs;
        private final Collection<URL> pathURLs;

        private static boolean isSupported() {
            return UnsafeAccess.UNSAFE != null;
        }

        UnsafeAccess(URLClassLoader classLoader) {
            super(classLoader);
            Collection<URL> unopened;
            Collection<URL> path;
            try {
                Object ucp = fetchField(URLClassLoader.class, classLoader, "ucp");
                unopened = (Collection<URL>) fetchField(ucp.getClass(), ucp, "unopenedUrls");
                path = (Collection<URL>) fetchField(ucp.getClass(), ucp, "path");
            } catch (Throwable t) {
                unopened = null;
                path = null;
            }
            this.unopenedURLs = unopened;
            this.pathURLs = path;
        }

        private static Object fetchField(Class<?> clazz, Object object, String name) throws NoSuchFieldException {
            Field field = clazz.getDeclaredField(name);
            long offset = UnsafeAccess.UNSAFE.objectFieldOffset(field);
            return UnsafeAccess.UNSAFE.getObject(object, offset);
        }

        @Override
        public void addURL(URL url) {
            if (this.unopenedURLs != null) {
                this.unopenedURLs.add(url);
            }
            if (this.pathURLs != null) {
                this.pathURLs.add(url);
            }
        }

        static {
            sun.misc.Unsafe unsafe;
            try {
                Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            } catch (Throwable t) {
                unsafe = null;
            }
            UNSAFE = unsafe;
        }
    }

    private static class Noop extends URLClassLoaderAccess {
        private static final Noop INSTANCE = new Noop();

        private Noop() {
            super(null);
        }

        @Override
        public void addURL(URL url) {
            throw new UnsupportedOperationException();
        }
    }
}
```

### 3) 启动时触发（示意）

```java
CompletableFuture
    .supplyAsync(() -> DependencyDownloader.downloadAndLoad(
        "com.zaxxer", "HikariCP", "4.0.3", new File("libraries")))
    .whenComplete((ok, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
        if (err != null || !Boolean.TRUE.equals(ok)) {
            plugin.getLogger().severe("Dependency download failed.");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return;
        }
        // init...
    }));
```
