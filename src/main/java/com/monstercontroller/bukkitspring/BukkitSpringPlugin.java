package com.monstercontroller.bukkitspring;

import com.monstercontroller.bukkitspring.api.ApplicationContext;
import com.monstercontroller.bukkitspring.api.StarterRegistry;
import com.monstercontroller.bukkitspring.dependency.BukkitSpringDependencies;
import com.monstercontroller.bukkitspring.dependency.DependencyDownloader;
import com.monstercontroller.bukkitspring.dependency.MavenDependency;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public final class BukkitSpringPlugin extends JavaPlugin {
    private ApplicationContext context;
    private boolean dependenciesReady = true;

    @Override
    public void onLoad() {
        // 1. 首先加载所有 Starter（触发它们的静态块注册）
        loadStarters();
        
        // 2. 然后下载依赖
        dependenciesReady = ensureDependencies();
    }

    @Override
    public void onEnable() {
        if (!dependenciesReady) {
            return;
        }
        saveDefaultConfig();
        
        // 创建内部上下文
        // 1. 获取 Starter 注册的配置类和扫描包
        List<Class<?>> configClasses = StarterRegistry.getAllConfigurations();
        List<String> scanPackages = StarterRegistry.getAllScanPackages();
        
        getLogger().info("Found " + configClasses.size() + " configuration classes from starters");
        getLogger().info("Found " + scanPackages.size() + " scan packages from starters");
        
        // 2. 创建上下文（通过扫描包）
        if (!scanPackages.isEmpty()) {
            context = BukkitSpring.registerPlugin(this, scanPackages.toArray(new String[0]));
        } else {
            context = BukkitSpring.registerPlugin(this);
        }
        
        // 3. 刷新上下文（会自动扫描并创建所有 @Configuration 类的 bean）
        context.refresh();
        
        getLogger().info("BukkitSpring enabled. Waiting for plugins to register.");
    }
    
    @Override
    public void onDisable() {
        // 通过 BukkitSpring 统一清理所有上下文（会调用 PreDestroy）
        BukkitSpring.shutdownAll();
        
        // 清理所有全局 Bean
        BukkitSpring.clearAllGlobalBeans();
    }

    private void loadStarters() {
        // 动态扫描 starters 目录
        java.io.File startersDir = new java.io.File(getDataFolder().getParentFile(), "BukkitSpring/starters");
        if (!startersDir.exists()) {
            startersDir.mkdirs();
            getLogger().info("Created starters directory: " + startersDir.getAbsolutePath());
            return;
        }
        
        java.io.File[] jarFiles = startersDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            getLogger().info("No starter jars found in starters directory");
            return;
        }
        
        getLogger().info("Found " + jarFiles.length + " starter jar(s) in starters directory");
        
        // 创建 DependencyDownloader 用于加载 jar
        DependencyDownloader downloader = new DependencyDownloader(this);
        
        for (java.io.File jarFile : jarFiles) {
            try {
                // 使用 DependencyDownloader 将 Starter jar 添加到 classpath
                java.net.URL jarUrl = jarFile.toURI().toURL();
                downloader.addJarToClasspath(jarUrl);
                
                // 尝试查找并加载 Starter 入口类
                // 约定：每个 Starter jar 的 META-INF/MANIFEST.MF 中定义 Starter-Class
                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
                    java.util.jar.Manifest manifest = jar.getManifest();
                    if (manifest != null) {
                        String starterClass = manifest.getMainAttributes().getValue("Starter-Class");
                        if (starterClass != null && !starterClass.isEmpty()) {
                            Class.forName(starterClass, true, getClass().getClassLoader());
                            getLogger().info("Loaded starter from " + jarFile.getName() + ": " + starterClass);
                        } else {
                            getLogger().warning("Starter jar " + jarFile.getName() + " does not define Starter-Class in MANIFEST.MF");
                        }
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Failed to load starter from " + jarFile.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean ensureDependencies() {
        DependencyDownloader downloader = new DependencyDownloader(this);
        try {
            // 注册 BukkitSpring 核心依赖
            StarterRegistry.registerDependencies(BukkitSpringDependencies.required());
            
            // 获取所有已注册的依赖（包括 Starter 注册的）
            List<MavenDependency> allDependencies = StarterRegistry.getAllDependencies();
            
            getLogger().info("Total dependencies to download: " + allDependencies.size());
            
            // 下载所有依赖
            downloader.ensureDependencies(allDependencies);
            return true;
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Dependency download failed. Disabling plugin.", ex);
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
    }
}