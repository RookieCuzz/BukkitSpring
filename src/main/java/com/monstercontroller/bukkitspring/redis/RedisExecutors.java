package com.monstercontroller.bukkitspring.redis;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

final class RedisExecutors {
    private RedisExecutors() {
    }

    static ExecutorService create(boolean preferVirtual, Logger logger) {
        if (preferVirtual) {
            ExecutorService virtual = createVirtualExecutor();
            if (virtual != null) {
                if (logger != null) {
                    logger.info("Redis executor uses virtual threads.");
                }
                return virtual;
            }
            if (logger != null) {
                logger.info("Virtual threads unavailable, using cached thread pool for Redis.");
            }
        }
        return Executors.newCachedThreadPool(new NamedThreadFactory("bukkitspring-redis-"));
    }

    private static ExecutorService createVirtualExecutor() {
        try {
            Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            Object executor = method.invoke(null);
            if (executor instanceof ExecutorService) {
                return (ExecutorService) executor;
            }
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
        return null;
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, prefix + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
