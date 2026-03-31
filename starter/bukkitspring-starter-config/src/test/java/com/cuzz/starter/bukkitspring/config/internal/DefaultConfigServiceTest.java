package com.cuzz.starter.bukkitspring.config.internal;

import com.cuzz.bukkitspring.spi.platform.PluginResource;
import com.cuzz.bukkitspring.spi.platform.PluginResourceResolver;
import com.cuzz.starter.bukkitspring.config.api.ConfigDocument;
import com.cuzz.starter.bukkitspring.config.api.ConfigQuery;
import com.cuzz.starter.bukkitspring.config.config.ConfigStarterSettings;
import com.cuzz.starter.bukkitspring.config.testutil.MapConfigView;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultConfigServiceTest {

    @Test
    void shouldLoadFromLocalFile() throws Exception {
        Path tempDir = Files.createTempDirectory("config-starter-local");
        Path file = tempDir.resolve("demo.yml");
        Files.writeString(file, "value: 1", StandardCharsets.UTF_8);

        ConfigStarterSettings settings = ConfigStarterSettings.fromConfig(new MapConfigView(Map.of(
                "config-starter.enabled", true,
                "config-starter.source.strategy", "local-only",
                "config-starter.source.local.enabled", true,
                "config-starter.source.remote.enabled", false
        )));

        DefaultConfigService service = new DefaultConfigService(settings, Logger.getLogger("test"), tempDir, null);
        ConfigDocument document = service.load("demo.yml");

        assertEquals("demo.yml", document.name());
        assertEquals("local-file", document.source());
        assertTrue(document.content().contains("value: 1"));
        service.close();
    }

    @Test
    void shouldFallbackToRemoteWhenLocalMissing() throws Exception {
        Path tempDir = Files.createTempDirectory("config-starter-remote");
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/remote.yml", this::writeYamlResponse);
        server.start();
        int port = server.getAddress().getPort();

        ConfigStarterSettings settings = ConfigStarterSettings.fromConfig(new MapConfigView(Map.of(
                "config-starter.enabled", true,
                "config-starter.source.strategy", "local-first",
                "config-starter.source.local.enabled", true,
                "config-starter.source.remote.enabled", true,
                "config-starter.source.remote.base-url", "http://127.0.0.1:" + port,
                "config-starter.source.remote.path-template", "{name}"
        )));

        DefaultConfigService service = new DefaultConfigService(settings, Logger.getLogger("test"), tempDir, null);
        ConfigDocument document = service.load("remote.yml");

        assertEquals("remote-http", document.source());
        assertTrue(document.content().contains("remote: true"));

        service.close();
        server.stop(0);
    }

    @Test
    void shouldUseCacheUnlessBypassRequested() throws Exception {
        Path tempDir = Files.createTempDirectory("config-starter-cache");
        Path file = tempDir.resolve("cache.yml");
        Files.writeString(file, "value: old", StandardCharsets.UTF_8);

        ConfigStarterSettings settings = ConfigStarterSettings.fromConfig(new MapConfigView(Map.of(
                "config-starter.enabled", true,
                "config-starter.cache.enabled", true,
                "config-starter.cache.ttl-ms", 60_000L,
                "config-starter.source.strategy", "local-only",
                "config-starter.source.local.enabled", true
        )));

        DefaultConfigService service = new DefaultConfigService(settings, Logger.getLogger("test"), tempDir, null);
        ConfigDocument first = service.load("cache.yml");

        Files.writeString(file, "value: new", StandardCharsets.UTF_8);

        ConfigDocument cached = service.load("cache.yml");
        ConfigDocument bypassed = service.load(ConfigQuery.builder("cache.yml").bypassCache(true).build());

        assertTrue(first.content().contains("value: old"));
        assertTrue(cached.content().contains("value: old"));
        assertTrue(bypassed.content().contains("value: new"));

        service.close();
    }

    @Test
    void shouldLoadFromPluginDataDirectoryWhenPluginSpecified() throws Exception {
        Path hostDir = Files.createTempDirectory("config-starter-host");
        Path pluginDir = Files.createTempDirectory("config-starter-plugin");
        Files.writeString(hostDir.resolve("menu_icons.yml"), "source: host", StandardCharsets.UTF_8);
        Files.writeString(pluginDir.resolve("menu_icons.yml"), "source: plugin", StandardCharsets.UTF_8);

        ConfigStarterSettings settings = ConfigStarterSettings.fromConfig(new MapConfigView(Map.of(
                "config-starter.enabled", true,
                "config-starter.source.strategy", "local-only",
                "config-starter.source.local.enabled", true,
                "config-starter.source.remote.enabled", false
        )));

        PluginResourceResolver resolver = new FixedPluginResourceResolver(List.of(
                new FixedPluginResource("RookieFortuneTree", pluginDir)
        ));

        DefaultConfigService service = new DefaultConfigService(settings, Logger.getLogger("test"), hostDir, resolver);
        ConfigDocument document = service.load(
                ConfigQuery.builder("menu_icons.yml").plugin("RookieFortuneTree").build()
        );

        assertEquals("local-file", document.source());
        assertTrue(document.content().contains("source: plugin"));
        service.close();
    }

    @Test
    void shouldFailWhenPluginSpecifiedButNotResolved() throws Exception {
        Path hostDir = Files.createTempDirectory("config-starter-host-missing");
        Files.writeString(hostDir.resolve("menu_icons.yml"), "source: host", StandardCharsets.UTF_8);

        ConfigStarterSettings settings = ConfigStarterSettings.fromConfig(new MapConfigView(Map.of(
                "config-starter.enabled", true,
                "config-starter.source.strategy", "local-only",
                "config-starter.source.local.enabled", true,
                "config-starter.source.remote.enabled", false
        )));

        DefaultConfigService service = new DefaultConfigService(
                settings,
                Logger.getLogger("test"),
                hostDir,
                new FixedPluginResourceResolver(List.of())
        );

        assertThrows(IllegalStateException.class, () ->
                service.load(ConfigQuery.builder("menu_icons.yml").plugin("RookieFortuneTree").build())
        );
        service.close();
    }

    private void writeYamlResponse(HttpExchange exchange) throws IOException {
        byte[] payload = "remote: true".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/yaml; charset=UTF-8");
        exchange.sendResponseHeaders(200, payload.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(payload);
        }
    }

    private static final class FixedPluginResourceResolver implements PluginResourceResolver {
        private final List<PluginResource> resources;

        private FixedPluginResourceResolver(List<PluginResource> resources) {
            this.resources = resources;
        }

        @Override
        public List<PluginResource> listPlugins() {
            return resources;
        }
    }

    private static final class FixedPluginResource implements PluginResource {
        private final String name;
        private final Path dataDirectory;

        private FixedPluginResource(String name, Path dataDirectory) {
            this.name = name;
            this.dataDirectory = dataDirectory;
        }

        @Override
        public Object getKey() {
            return name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public ClassLoader getClassLoader() {
            return DefaultConfigServiceTest.class.getClassLoader();
        }

        @Override
        public String getMainPackage() {
            return "test";
        }

        @Override
        public Path getSourcePath() {
            return dataDirectory;
        }

        @Override
        public InputStream openResource(String path) {
            return null;
        }

        @Override
        public Path getDataDirectory() {
            return dataDirectory;
        }
    }
}
