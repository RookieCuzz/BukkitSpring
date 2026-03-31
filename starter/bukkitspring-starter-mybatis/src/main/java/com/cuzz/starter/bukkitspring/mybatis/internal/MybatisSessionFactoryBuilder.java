package com.cuzz.starter.bukkitspring.mybatis.internal;

import com.cuzz.starter.bukkitspring.mybatis.config.MybatisSettings;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.util.Locale;
import java.util.logging.Logger;

final class MybatisSessionFactoryBuilder {
    SqlSessionFactory build(MybatisSettings settings, Logger logger) {
        HikariDataSource dataSource = buildDataSource(settings);
        Configuration configuration = buildConfiguration(settings, dataSource, logger);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private HikariDataSource buildDataSource(MybatisSettings settings) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(settings.jdbcUrl);
        if (!settings.username.isEmpty()) {
            dataSource.setUsername(settings.username);
        }
        if (!settings.password.isEmpty()) {
            dataSource.setPassword(settings.password);
        }
        if (!settings.driverClassName.isEmpty()) {
            dataSource.setDriverClassName(settings.driverClassName);
        }
        dataSource.setMaximumPoolSize(settings.poolMaxSize);
        dataSource.setMinimumIdle(settings.poolMinIdle);
        dataSource.setConnectionTimeout(settings.connectionTimeoutMs);
        if (settings.poolMaxLifetimeMs > 0) {
            dataSource.setMaxLifetime(settings.poolMaxLifetimeMs);
        }
        if (settings.poolIdleTimeoutMs > 0) {
            dataSource.setIdleTimeout(settings.poolIdleTimeoutMs);
        }
        if (settings.poolKeepaliveTimeMs > 0) {
            dataSource.setKeepaliveTime(settings.poolKeepaliveTimeMs);
        }
        if (settings.poolValidationTimeoutMs > 0) {
            dataSource.setValidationTimeout(settings.poolValidationTimeoutMs);
        }
        if (settings.poolLeakDetectionThresholdMs > 0) {
            dataSource.setLeakDetectionThreshold(settings.poolLeakDetectionThresholdMs);
        }
        dataSource.setAutoCommit(settings.autoCommit);
        return dataSource;
    }

    private Configuration buildConfiguration(MybatisSettings settings,
                                             HikariDataSource dataSource,
                                             Logger logger) {
        Environment environment = new Environment("default", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        Class<? extends Log> logImpl = settings.debug
                ? resolveLogImpl(settings.logImpl, logger)
                : resolveLogImpl("NO_LOGGING", logger);
        if (logImpl != null) {
            configuration.setLogImpl(logImpl);
        }
        if (settings.slowSqlThresholdMs > 0) {
            configuration.addInterceptor(new SlowSqlInterceptor(settings.slowSqlThresholdMs, logger));
        }
        return configuration;
    }

    private Class<? extends Log> resolveLogImpl(String value, Logger logger) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        String candidate = trimmed.contains(".") ? trimmed : switch (trimmed.toUpperCase(Locale.ROOT)) {
            case "STDOUT_LOGGING", "STDOUT" -> "org.apache.ibatis.logging.stdout.StdOutImpl";
            case "NO_LOGGING", "NO_LOG" -> "org.apache.ibatis.logging.nologging.NoLoggingImpl";
            case "SLF4J" -> "org.apache.ibatis.logging.slf4j.Slf4jImpl";
            case "LOG4J" -> "org.apache.ibatis.logging.log4j.Log4jImpl";
            case "LOG4J2" -> "org.apache.ibatis.logging.log4j2.Log4j2Impl";
            case "JDK_LOGGING", "JUL" -> "org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl";
            case "COMMONS_LOGGING", "COMMONS" -> "org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl";
            default -> trimmed;
        };
        try {
            Class<?> type = Class.forName(candidate, false, MybatisSessionFactoryBuilder.class.getClassLoader());
            if (Log.class.isAssignableFrom(type)) {
                return type.asSubclass(Log.class);
            }
        } catch (Exception ex) {
            if (logger != null) {
                logger.warning("[MyBatis] Unknown log-impl: " + value);
            }
        }
        return null;
    }
}
