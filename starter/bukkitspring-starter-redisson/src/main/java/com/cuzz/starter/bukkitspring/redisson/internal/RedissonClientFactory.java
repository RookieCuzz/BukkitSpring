package com.cuzz.starter.bukkitspring.redisson.internal;

import com.cuzz.starter.bukkitspring.redisson.api.RedissonMode;
import com.cuzz.starter.bukkitspring.redisson.config.RedissonClusterSettings;
import com.cuzz.starter.bukkitspring.redisson.config.RedissonSettings;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

final class RedissonClientFactory {

    RedissonClient create(RedissonSettings settings, Logger logger) {
        Objects.requireNonNull(settings, "settings");
        Config config = buildConfig(settings);
        if (logger != null) {
            logger.info("[Redisson] Creating RedissonClient in " + settings.mode + " mode.");
        }
        return Redisson.create(config);
    }

    private Config buildConfig(RedissonSettings settings) {
        Config config = new Config();
        config.setThreads(settings.threads);
        config.setNettyThreads(settings.nettyThreads);
        config.setTransportMode(settings.transportMode);

        if (settings.mode == RedissonMode.CLUSTER) {
            configureCluster(config, settings);
        } else {
            configureSingle(config, settings);
        }
        return config;
    }

    private void configureSingle(Config config, RedissonSettings settings) {
        SingleServerConfig single = config.useSingleServer();
        single.setAddress(defaultIfBlank(settings.address, "redis://127.0.0.1:6379"));
        single.setDatabase(settings.database);
        single.setTimeout(settings.timeoutMillis);
        single.setConnectTimeout(settings.connectTimeoutMillis);
        single.setIdleConnectionTimeout(settings.idleConnectionTimeoutMillis);
        single.setRetryAttempts(settings.retryAttempts);
        single.setRetryInterval(settings.retryIntervalMillis);
        single.setConnectionPoolSize(settings.connectionPoolSize);
        single.setConnectionMinimumIdleSize(settings.connectionMinimumIdleSize);
        single.setSubscriptionConnectionPoolSize(settings.subscriptionConnectionPoolSize);
        single.setSubscriptionConnectionMinimumIdleSize(settings.subscriptionConnectionMinimumIdleSize);
        setCommonCredentials(single, settings.username, settings.password, settings.clientName);
    }

    private void configureCluster(Config config, RedissonSettings settings) {
        ClusterServersConfig clusterConfig = config.useClusterServers();
        List<String> nodes = settings.cluster == null ? List.of() : settings.cluster.nodes;
        if (nodes == null || nodes.isEmpty()) {
            clusterConfig.addNodeAddress(defaultIfBlank(settings.address, "redis://127.0.0.1:6379"));
        } else {
            clusterConfig.addNodeAddress(nodes.toArray(new String[0]));
        }
        RedissonClusterSettings cluster = settings.cluster;
        if (cluster != null) {
            clusterConfig.setScanInterval(cluster.scanIntervalMillis);
        }
        clusterConfig.setTimeout(settings.timeoutMillis);
        clusterConfig.setConnectTimeout(settings.connectTimeoutMillis);
        clusterConfig.setIdleConnectionTimeout(settings.idleConnectionTimeoutMillis);
        clusterConfig.setRetryAttempts(settings.retryAttempts);
        clusterConfig.setRetryInterval(settings.retryIntervalMillis);
        clusterConfig.setMasterConnectionPoolSize(settings.connectionPoolSize);
        clusterConfig.setMasterConnectionMinimumIdleSize(settings.connectionMinimumIdleSize);
        clusterConfig.setSubscriptionConnectionPoolSize(settings.subscriptionConnectionPoolSize);
        clusterConfig.setSubscriptionConnectionMinimumIdleSize(settings.subscriptionConnectionMinimumIdleSize);
        setCommonCredentials(clusterConfig, settings.username, settings.password, settings.clientName);
    }

    private void setCommonCredentials(SingleServerConfig config, String username, String password, String clientName) {
        String normalizedUser = normalize(username);
        String normalizedPassword = normalize(password);
        String normalizedClientName = normalize(clientName);
        if (!normalizedUser.isEmpty()) {
            config.setUsername(normalizedUser);
        }
        if (!normalizedPassword.isEmpty()) {
            config.setPassword(normalizedPassword);
        }
        if (!normalizedClientName.isEmpty()) {
            config.setClientName(normalizedClientName);
        }
    }

    private void setCommonCredentials(ClusterServersConfig config, String username, String password, String clientName) {
        String normalizedUser = normalize(username);
        String normalizedPassword = normalize(password);
        String normalizedClientName = normalize(clientName);
        if (!normalizedUser.isEmpty()) {
            config.setUsername(normalizedUser);
        }
        if (!normalizedPassword.isEmpty()) {
            config.setPassword(normalizedPassword);
        }
        if (!normalizedClientName.isEmpty()) {
            config.setClientName(normalizedClientName);
        }
    }

    private static String defaultIfBlank(String value, String fallback) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
