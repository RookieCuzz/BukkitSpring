package com.cuzz.starter.bukkitspring.redis.internal;

import com.cuzz.starter.bukkitspring.redis.api.RedisMode;
import com.cuzz.starter.bukkitspring.redis.config.RedisSettings;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

final class RedisClientFactory {
    UnifiedJedis create(RedisSettings settings, Logger logger) {
        JedisClientConfig clientConfig = buildClientConfig(settings);
        GenericObjectPoolConfig<Connection> poolConfig = buildPoolConfig(settings);

        if (settings.mode == RedisMode.CLUSTER) {
            warnClusterDatabaseIgnored(settings, logger);
            return createClusterClient(settings, clientConfig, poolConfig);
        }
        return createStandaloneClient(settings, clientConfig, poolConfig);
    }

    private void warnClusterDatabaseIgnored(RedisSettings settings, Logger logger) {
        if (settings.database != 0 && logger != null) {
            logger.warning("[Redis] redis.database is ignored in cluster mode.");
        }
    }

    private JedisClientConfig buildClientConfig(RedisSettings settings) {
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(settings.connectionTimeoutMillis)
                .socketTimeoutMillis(settings.socketTimeoutMillis)
                .database(settings.database)
                .ssl(settings.ssl)
                .clientName(settings.clientName);
        if (!settings.user.isBlank()) {
            builder.user(settings.user);
        }
        if (!settings.password.isBlank()) {
            builder.password(settings.password);
        }
        return builder.build();
    }

    private GenericObjectPoolConfig<Connection> buildPoolConfig(RedisSettings settings) {
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(settings.poolMaxTotal);
        poolConfig.setMaxIdle(settings.poolMaxIdle);
        poolConfig.setMinIdle(settings.poolMinIdle);
        poolConfig.setMaxWaitMillis(settings.poolMaxWaitMillis);
        return poolConfig;
    }

    private UnifiedJedis createStandaloneClient(RedisSettings settings,
                                                JedisClientConfig clientConfig,
                                                GenericObjectPoolConfig<Connection> poolConfig) {
        HostAndPort hostAndPort = new HostAndPort(settings.host, settings.port);
        return new JedisPooled(poolConfig, hostAndPort, clientConfig);
    }

    private UnifiedJedis createClusterClient(RedisSettings settings,
                                             JedisClientConfig clientConfig,
                                             GenericObjectPoolConfig<Connection> poolConfig) {
        Set<HostAndPort> nodes = parseClusterNodes(settings, settings.cluster.nodes);
        int maxRedirects = settings.cluster.maxRedirects;
        long refreshMillis = settings.cluster.topologyRefreshMillis;
        if (refreshMillis > 0) {
            return new JedisCluster(nodes, clientConfig, maxRedirects, Duration.ofMillis(refreshMillis), poolConfig);
        }
        return new JedisCluster(nodes, clientConfig, maxRedirects, poolConfig);
    }

    private Set<HostAndPort> parseClusterNodes(RedisSettings settings, List<String> nodes) {
        Set<HostAndPort> hostAndPorts = new LinkedHashSet<>();
        if (nodes != null) {
            for (String node : nodes) {
                HostAndPort parsed = parseNode(settings, node);
                if (parsed != null) {
                    hostAndPorts.add(parsed);
                }
            }
        }
        if (hostAndPorts.isEmpty()) {
            hostAndPorts.add(new HostAndPort(settings.host, settings.port));
        }
        return hostAndPorts;
    }

    private HostAndPort parseNode(RedisSettings settings, String node) {
        if (node == null || node.isBlank()) {
            return null;
        }
        String trimmed = node.trim();
        String[] parts = trimmed.split(":", 2);
        String host = parts[0].trim();
        if (host.isEmpty()) {
            return null;
        }
        int port = settings.port;
        if (parts.length > 1 && !parts[1].isBlank()) {
            port = safePort(parts[1].trim(), settings.port);
        }
        return new HostAndPort(host, port);
    }

    private int safePort(String value, int fallback) {
        try {
            int port = Integer.parseInt(value);
            if (port >= 1 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
            return fallback;
        }
        return fallback;
    }
}
