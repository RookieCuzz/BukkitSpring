package com.cuzz.starter.bukkitspring.mybatis.internal;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
final class SlowSqlInterceptor implements Interceptor {
    private final long thresholdMs;
    private final Logger logger;

    SlowSqlInterceptor(long thresholdMs, Logger logger) {
        this.thresholdMs = thresholdMs;
        this.logger = logger;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            if (elapsedMs >= thresholdMs) {
                logSlow(invocation, elapsedMs);
            }
        }
    }

    private void logSlow(Invocation invocation, long elapsedMs) {
        if (logger == null) {
            return;
        }
        Object[] args = invocation.getArgs();
        if (args.length < 2) {
            logger.warning("[MyBatis] Slow SQL (" + elapsedMs + "ms) args=" + args.length);
            return;
        }
        MappedStatement mappedStatement = (MappedStatement) args[0];
        Object parameter = args[1];
        BoundSql boundSql = null;
        if (args.length >= 6 && args[5] instanceof BoundSql) {
            boundSql = (BoundSql) args[5];
        }
        if (boundSql == null) {
            boundSql = mappedStatement.getBoundSql(parameter);
        }
        String sql = boundSql == null ? "" : normalizeSql(boundSql.getSql());
        String paramText = formatParameters(mappedStatement, boundSql, parameter);
        logger.warning("[MyBatis] Slow SQL (" + elapsedMs + "ms) id=" + mappedStatement.getId()
                + " sql=\"" + sql + "\" params=" + paramText);
    }

    private String formatParameters(MappedStatement statement, BoundSql boundSql, Object parameter) {
        if (boundSql == null || statement == null) {
            return formatValue(parameter);
        }
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        if (mappings == null || mappings.isEmpty()) {
            return formatValue(parameter);
        }
        TypeHandlerRegistry registry = statement.getConfiguration().getTypeHandlerRegistry();
        MetaObject metaObject = parameter == null ? null : statement.getConfiguration().newMetaObject(parameter);
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (ParameterMapping mapping : mappings) {
            if (mapping.getMode() == ParameterMode.OUT) {
                continue;
            }
            String property = mapping.getProperty();
            Object value;
            if (boundSql.hasAdditionalParameter(property)) {
                value = boundSql.getAdditionalParameter(property);
            } else if (parameter == null) {
                value = null;
            } else if (registry.hasTypeHandler(parameter.getClass())) {
                value = parameter;
            } else if (metaObject != null && metaObject.hasGetter(property)) {
                value = metaObject.getValue(property);
            } else {
                value = null;
            }
            joiner.add(property + "=" + formatValue(value));
        }
        return joiner.toString();
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence) {
            return "\"" + value + "\"";
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            int length = Array.getLength(value);
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            for (int i = 0; i < length; i++) {
                joiner.add(formatValue(Array.get(value, i)));
            }
            return joiner.toString();
        }
        if (value instanceof Collection<?> collection) {
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            for (Object item : collection) {
                joiner.add(formatValue(item));
            }
            return joiner.toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringJoiner joiner = new StringJoiner(", ", "{", "}");
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                joiner.add(formatValue(entry.getKey()) + "=" + formatValue(entry.getValue()));
            }
            return joiner.toString();
        }
        return String.valueOf(value);
    }

    private String normalizeSql(String sql) {
        if (sql == null) {
            return "";
        }
        return sql.replaceAll("\\s+", " ").trim();
    }
}
