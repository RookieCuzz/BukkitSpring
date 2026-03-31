package com.cuzz.starter.bukkitspring.mybatis.internal.mapper;

import com.cuzz.starter.bukkitspring.mybatis.core.MybatisService;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

public final class MybatisMapperProxy {
    private MybatisMapperProxy() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> mapperType, MybatisService service) {
        Objects.requireNonNull(mapperType, "mapperType");
        Objects.requireNonNull(service, "service");
        if (!mapperType.isInterface()) {
            throw new IllegalArgumentException("Mapper type must be an interface: " + mapperType.getName());
        }
        InvocationHandler handler = new MapperInvocationHandler(mapperType, service);
        return (T) Proxy.newProxyInstance(
                mapperType.getClassLoader(),
                new Class<?>[]{mapperType},
                handler
        );
    }

    private static final class MapperInvocationHandler implements InvocationHandler {
        private final Class<?> mapperType;
        private final MybatisService service;

        private MapperInvocationHandler(Class<?> mapperType, MybatisService service) {
            this.mapperType = mapperType;
            this.service = service;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }
            if (!service.isEnabled()) {
                throw new IllegalStateException("MyBatis is not enabled.");
            }
            try (SqlSession session = service.openSession(service.isAutoCommit())) {
                Object mapper = session.getMapper(mapperType);
                try {
                    return method.invoke(mapper, args);
                } catch (InvocationTargetException ex) {
                    throw ex.getTargetException();
                }
            }
        }

        private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("toString".equals(name) && method.getParameterCount() == 0) {
                return "MybatisMapperProxy(" + mapperType.getName() + ")";
            }
            if ("hashCode".equals(name) && method.getParameterCount() == 0) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name) && method.getParameterCount() == 1) {
                return proxy == args[0];
            }
            try {
                return method.invoke(this, args);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to invoke mapper method: " + name, ex);
            }
        }
    }
}
