package com.monstercontroller.bukkitspring.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

abstract class InjectionPoint {
    abstract void inject(SimpleApplicationContext context, Object instance);

    static final class FieldInjectionPoint extends InjectionPoint {
        private final Field field;

        FieldInjectionPoint(Field field) {
            this.field = field;
        }

        @Override
        void inject(SimpleApplicationContext context, Object instance) {
            Object value = context.resolveDependency(field, field.getType(), field.getGenericType());
            try {
                field.setAccessible(true);
                field.set(instance, value);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Failed to inject field: " + field, ex);
            }
        }
    }

    static final class MethodInjectionPoint extends InjectionPoint {
        private final Method method;

        MethodInjectionPoint(Method method) {
            this.method = method;
        }

        @Override
        void inject(SimpleApplicationContext context, Object instance) {
            Object[] args = context.resolveExecutableArguments(method, method.getParameters(), method.getGenericParameterTypes());
            try {
                method.setAccessible(true);
                method.invoke(instance, args);
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Failed to inject method: " + method, ex);
            }
        }
    }
}
