package com.cuzz.starter.bukkitspring.redisson.internal;

import com.cuzz.starter.bukkitspring.redisson.config.RedissonSettings;
import com.cuzz.starter.bukkitspring.redisson.testutil.MapConfigView;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DefaultRedissonServiceTest {

    @Test
    public void executorShouldNotBeInitializedAfterCloseRace() throws Exception {
        DefaultRedissonService service = new DefaultRedissonService(enabledSettings(), Logger.getLogger("test-redisson"));
        Object executorLock = readField(service, "executorLock");

        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            started.countDown();
            try {
                service.executor();
                outcome.set(new AssertionError("Expected IllegalStateException after close."));
            } catch (Throwable ex) {
                outcome.set(ex);
            }
        }, "redisson-executor-race");

        synchronized (executorLock) {
            thread.start();
            assertTrue(started.await(1, TimeUnit.SECONDS));
            waitUntilBlocked(thread);
            service.close();
        }

        thread.join(2000);
        assertFalse(thread.isAlive());
        assertTrue(outcome.get() instanceof IllegalStateException);
        assertNull(readField(service, "executor"));
    }

    @Test
    public void clientShouldNotBeInitializedAfterCloseRace() throws Exception {
        DefaultRedissonService service = new DefaultRedissonService(enabledSettings(), Logger.getLogger("test-redisson"));
        Object clientLock = readField(service, "clientLock");

        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            started.countDown();
            try {
                service.client();
                outcome.set(new AssertionError("Expected IllegalStateException after close."));
            } catch (Throwable ex) {
                outcome.set(ex);
            }
        }, "redisson-client-race");

        synchronized (clientLock) {
            thread.start();
            assertTrue(started.await(1, TimeUnit.SECONDS));
            waitUntilBlocked(thread);
            service.close();
        }

        thread.join(5000);
        assertFalse(thread.isAlive());
        assertTrue(outcome.get() instanceof IllegalStateException);

        Object maybeClient = readField(service, "client");
        if (maybeClient instanceof RedissonClient redissonClient) {
            redissonClient.shutdown();
            fail("Client should not be created after service close.");
        }
    }

    private static RedissonSettings enabledSettings() {
        return RedissonSettings.fromConfig(new MapConfigView(
                Map.of("redisson", Map.of("enabled", true))
        ));
    }

    private static Object readField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void waitUntilBlocked(Thread thread) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            Thread.State state = thread.getState();
            if (state == Thread.State.BLOCKED) {
                return;
            }
            Thread.sleep(10L);
        }
        fail("Thread did not enter BLOCKED state in time.");
    }
}

