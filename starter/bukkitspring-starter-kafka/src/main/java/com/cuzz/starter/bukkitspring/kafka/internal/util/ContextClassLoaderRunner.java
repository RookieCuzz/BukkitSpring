package com.cuzz.starter.bukkitspring.kafka.internal.util;

import java.util.Objects;
import java.util.function.Supplier;

public final class ContextClassLoaderRunner {
    private ContextClassLoaderRunner() {
    }

    public static <T> T runWith(ClassLoader target, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        if (target == null || target == previous) {
            return action.get();
        }
        thread.setContextClassLoader(target);
        try {
            return action.get();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }
}
