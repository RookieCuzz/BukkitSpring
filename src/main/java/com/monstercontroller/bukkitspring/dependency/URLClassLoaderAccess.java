package com.monstercontroller.bukkitspring.dependency;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

public abstract class URLClassLoaderAccess {
    public static URLClassLoaderAccess create(URLClassLoader classLoader) {
        if (UnsafeAccess.isSupported()) {
            return new UnsafeAccess(classLoader);
        }
        return Noop.INSTANCE;
    }

    public abstract void addURL(URL url);

    private static class UnsafeAccess extends URLClassLoaderAccess {
        private static final Unsafe UNSAFE = loadUnsafe();
        private final Collection<URL> unopenedUrls;
        private final Collection<URL> pathUrls;

        private static boolean isSupported() {
            return UNSAFE != null;
        }

        @SuppressWarnings("unchecked")
        UnsafeAccess(URLClassLoader classLoader) {
            Collection<URL> unopened = null;
            Collection<URL> path = null;
            try {
                Object ucp = fetchField(URLClassLoader.class, classLoader, "ucp");
                unopened = (Collection<URL>) fetchField(ucp.getClass(), ucp, "unopenedUrls");
                path = (Collection<URL>) fetchField(ucp.getClass(), ucp, "path");
            } catch (Throwable ignored) {
                unopened = null;
                path = null;
            }
            this.unopenedUrls = unopened;
            this.pathUrls = path;
        }

        private static Object fetchField(Class<?> clazz, Object target, String name) throws NoSuchFieldException {
            Field field = clazz.getDeclaredField(name);
            long offset = UNSAFE.objectFieldOffset(field);
            return UNSAFE.getObject(target, offset);
        }

        @Override
        public void addURL(URL url) {
            if (unopenedUrls != null) {
                unopenedUrls.add(url);
            }
            if (pathUrls != null) {
                pathUrls.add(url);
            }
            if (unopenedUrls == null && pathUrls == null) {
                throw new UnsupportedOperationException("URLClassLoader access not available.");
            }
        }

        private static Unsafe loadUnsafe() {
            try {
                Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                return (Unsafe) unsafeField.get(null);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static class Noop extends URLClassLoaderAccess {
        private static final Noop INSTANCE = new Noop();

        @Override
        public void addURL(URL url) {
            throw new UnsupportedOperationException("URLClassLoader access not available.");
        }
    }
}
