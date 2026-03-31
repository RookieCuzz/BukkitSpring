package com.cuzz.starter.bukkitspring.redisson;

import com.cuzz.starter.bukkitspring.redisson.api.RedissonService;
import com.cuzz.starter.bukkitspring.redisson.config.RedissonSettings;
import org.junit.jupiter.api.Test;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RIdGenerator;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RLock;
import org.redisson.api.RQueue;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RRemoteService;
import org.redisson.api.RScheduledExecutorService;
import org.redisson.api.RSearch;
import org.redisson.api.RScript;
import org.redisson.api.RTransaction;
import org.redisson.api.RedissonClient;
import org.redisson.api.TransactionOptions;
import org.redisson.api.options.ExecutorOptions;
import org.redisson.api.options.LocalCachedMapOptions;
import org.redisson.api.options.OptionalOptions;
import org.redisson.api.options.PlainOptions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RedissonServiceInterfaceBehaviorTest {

    @Test
    public void withLockShouldAcquireAndRelease() {
        LockStub lock = new LockStub();
        ClientStub client = new ClientStub(lock);
        TestRedissonService service = new TestRedissonService(client.proxy);

        AtomicInteger counter = new AtomicInteger();
        service.withLock("demo:lock", counter::incrementAndGet);

        assertEquals(1, counter.get());
        assertEquals(1, lock.lockCalls.get());
        assertEquals(1, lock.unlockCalls.get());
        assertEquals("demo:lock", client.lastLockName.get());

        service.close();
    }

    @Test
    public void withLockShouldReleaseWhenActionThrows() {
        LockStub lock = new LockStub();
        ClientStub client = new ClientStub(lock);
        TestRedissonService service = new TestRedissonService(client.proxy);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.withLock("demo:lock", () -> {
                    throw new RuntimeException("boom");
                }));

        assertEquals("boom", ex.getMessage());
        assertEquals(1, lock.lockCalls.get());
        assertEquals(1, lock.unlockCalls.get());

        service.close();
    }

    @Test
    public void tryWithLockShouldRespectAcquireResult() {
        LockStub lock = new LockStub();
        ClientStub client = new ClientStub(lock);
        TestRedissonService service = new TestRedissonService(client.proxy);

        lock.tryLockResult = false;
        AtomicInteger counter = new AtomicInteger();
        boolean acquired = service.tryWithLock("demo:try", 10L, 50L, counter::incrementAndGet);
        assertFalse(acquired);
        assertEquals(0, counter.get());
        assertEquals(0, lock.unlockCalls.get());

        lock.tryLockResult = true;
        String value = service.tryWithLock("demo:try", 10L, 50L, () -> "ok", "fallback");
        assertEquals("ok", value);
        assertEquals(1, lock.unlockCalls.get());

        service.close();
    }

    @Test
    public void wrappersShouldDelegateToClient() {
        LockStub lock = new LockStub();
        ClientStub client = new ClientStub(lock);
        TestRedissonService service = new TestRedissonService(client.proxy);

        RRateLimiter rateLimiter = service.getRateLimiter("demo:rl");
        assertSame(client.rateLimiterProxy, rateLimiter);
        assertEquals("demo:rl", client.lastRateLimiterName.get());

        RLocalCachedMap<Object, Object> localMap = service.getLocalCachedMap("demo:local");
        assertSame(client.localCachedMapProxy, localMap);
        assertNotNull(client.lastLocalMapOptions.get());
        assertEquals(1, client.localMapOptionsOnlyCalls.get());

        LocalCachedMapOptions<Object, Object> options = LocalCachedMapOptions.name("demo:local2");
        RLocalCachedMap<Object, Object> localMap2 = service.getLocalCachedMap(options);
        assertSame(client.localCachedMapProxy, localMap2);
        assertSame(options, client.lastLocalMapOptions.get());
        assertEquals(2, client.localMapOptionsOnlyCalls.get());

        RBatch batch = service.createBatch();
        assertSame(client.batchProxy, batch);
        service.createBatch(BatchOptions.defaults());
        assertEquals(2, client.batchCalls.get());

        RScheduledExecutorService executorService = service.getExecutorService("demo:executor");
        assertSame(client.executorServiceProxy, executorService);
        assertEquals("demo:executor", client.lastExecutorServiceName.get());
        assertEquals(1, client.executorServiceNameCalls.get());

        ExecutorOptions executorOptions = ExecutorOptions.name("demo:executor2");
        RScheduledExecutorService executorService2 = service.getExecutorService(executorOptions);
        assertSame(client.executorServiceProxy, executorService2);
        assertSame(executorOptions, client.lastExecutorOptions.get());
        assertEquals(1, client.executorServiceOptionsCalls.get());

        RScript script = service.getScript();
        assertSame(client.scriptProxy, script);
        assertEquals(1, client.scriptCalls.get());

        RIdGenerator idGenerator = service.getIdGenerator("demo:id");
        assertSame(client.idGeneratorProxy, idGenerator);
        assertEquals("demo:id", client.lastIdGeneratorName.get());

        RBloomFilter<Object> bloomFilter = service.getBloomFilter("demo:bloom");
        assertSame(client.bloomFilterProxy, bloomFilter);
        assertEquals("demo:bloom", client.lastBloomFilterName.get());

        RRemoteService remoteService = service.getRemoteService("demo:remote");
        assertSame(client.remoteServiceProxy, remoteService);
        assertEquals("demo:remote", client.lastRemoteServiceName.get());
        assertEquals(1, client.remoteServiceNameCalls.get());

        PlainOptions remoteOptions = PlainOptions.name("demo:remote2");
        RRemoteService remoteService2 = service.getRemoteService(remoteOptions);
        assertSame(client.remoteServiceProxy, remoteService2);
        assertSame(remoteOptions, client.lastPlainOptions.get());
        assertEquals(1, client.remoteServiceOptionsCalls.get());

        RSearch search = service.getSearch();
        assertSame(client.searchProxy, search);
        assertEquals(1, client.searchDefaultCalls.get());

        OptionalOptions optionalOptions = OptionalOptions.defaults();
        RSearch search2 = service.getSearch(optionalOptions);
        assertSame(client.searchProxy, search2);
        assertSame(optionalOptions, client.lastOptionalOptions.get());
        assertEquals(1, client.searchOptionsCalls.get());

        service.createTransaction();
        service.createTransaction(TransactionOptions.defaults());
        assertEquals(2, client.transactionCalls.get());

        RDelayedQueue<Object> delayedQueue = service.getDelayedQueue("demo:q");
        assertSame(client.delayedQueueProxy, delayedQueue);
        assertEquals("demo:q", client.lastQueueName.get());

        service.destroyDelayedQueue("demo:q");
        assertEquals(1, client.delayedDestroyCalls.get());

        service.close();
    }

    @Test
    public void withTransactionShouldCommitOnSuccess() {
        LockStub lock = new LockStub();
        ClientStub client = new ClientStub(lock);
        TestRedissonService service = new TestRedissonService(client.proxy);

        String value = service.withTransaction(tx -> "ok");

        assertEquals("ok", value);
        assertEquals(1, client.transactionCalls.get());
        assertEquals(1, client.transactionCommitCalls.get());
        assertEquals(0, client.transactionRollbackCalls.get());

        service.close();
    }

    @Test
    public void withTransactionShouldRollbackOnFailure() {
        LockStub lock = new LockStub();
        ClientStub client = new ClientStub(lock);
        TestRedissonService service = new TestRedissonService(client.proxy);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.withTransaction(TransactionOptions.defaults(), tx -> {
                    throw new RuntimeException("tx-fail");
                }));

        assertEquals("tx-fail", ex.getMessage());
        assertEquals(1, client.transactionCalls.get());
        assertEquals(0, client.transactionCommitCalls.get());
        assertEquals(1, client.transactionRollbackCalls.get());

        service.close();
    }

    @Test
    public void optionsWrappersShouldRejectNull() {
        LockStub lock = new LockStub();
        ClientStub client = new ClientStub(lock);
        TestRedissonService service = new TestRedissonService(client.proxy);

        IllegalArgumentException executorEx = assertThrows(IllegalArgumentException.class,
                () -> service.getExecutorService((ExecutorOptions) null));
        assertEquals("ExecutorOptions must not be null.", executorEx.getMessage());

        IllegalArgumentException remoteEx = assertThrows(IllegalArgumentException.class,
                () -> service.getRemoteService((PlainOptions) null));
        assertEquals("PlainOptions must not be null.", remoteEx.getMessage());

        IllegalArgumentException searchEx = assertThrows(IllegalArgumentException.class,
                () -> service.getSearch((OptionalOptions) null));
        assertEquals("OptionalOptions must not be null.", searchEx.getMessage());

        service.close();
    }

    @Test
    public void getLocalCachedMapShouldRejectNullOptions() {
        LockStub lock = new LockStub();
        ClientStub client = new ClientStub(lock);
        TestRedissonService service = new TestRedissonService(client.proxy);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getLocalCachedMap((LocalCachedMapOptions<Object, Object>) null));

        assertEquals("LocalCachedMapOptions must not be null.", ex.getMessage());

        service.close();
    }

    private static final class TestRedissonService implements RedissonService {
        private final RedissonClient client;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final RedissonSettings settings = RedissonSettings.fromConfig(null);
        private volatile boolean closed;

        private TestRedissonService(RedissonClient client) {
            this.client = Objects.requireNonNull(client, "client");
        }

        @Override
        public boolean isEnabled() {
            return !closed;
        }

        @Override
        public RedissonSettings settings() {
            return settings;
        }

        @Override
        public ExecutorService executor() {
            return executor;
        }

        @Override
        public RedissonClient client() {
            return client;
        }

        @Override
        public void close() {
            closed = true;
            executor.shutdown();
        }
    }

    private static final class LockStub implements InvocationHandler {
        private final RLock proxy = newProxy(RLock.class, this);
        private final AtomicInteger lockCalls = new AtomicInteger();
        private final AtomicInteger unlockCalls = new AtomicInteger();
        private volatile boolean tryLockResult = true;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("lock".equals(name)) {
                lockCalls.incrementAndGet();
                return null;
            }
            if ("unlock".equals(name)) {
                unlockCalls.incrementAndGet();
                return null;
            }
            if ("tryLock".equals(name)) {
                return tryLockResult;
            }
            if ("toString".equals(name)) {
                return "LockStub";
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static final class ClientStub implements InvocationHandler {
        private final RedissonClient proxy = newProxy(RedissonClient.class, this);
        private final RRateLimiter rateLimiterProxy = newProxy(RRateLimiter.class, this::simpleInvoke);
        private final RQueue<Object> queueProxy = newProxy(RQueue.class, this::simpleInvoke);
        private final RLocalCachedMap<Object, Object> localCachedMapProxy = newProxy(RLocalCachedMap.class, this::simpleInvoke);
        private final RBatch batchProxy = newProxy(RBatch.class, this::simpleInvoke);
        private final RScheduledExecutorService executorServiceProxy = newProxy(RScheduledExecutorService.class, this::simpleInvoke);
        private final RScript scriptProxy = newProxy(RScript.class, this::simpleInvoke);
        private final RIdGenerator idGeneratorProxy = newProxy(RIdGenerator.class, this::simpleInvoke);
        private final RBloomFilter<Object> bloomFilterProxy = newProxy(RBloomFilter.class, this::simpleInvoke);
        private final RRemoteService remoteServiceProxy = newProxy(RRemoteService.class, this::simpleInvoke);
        private final RSearch searchProxy = newProxy(RSearch.class, this::simpleInvoke);
        private final AtomicInteger transactionCommitCalls = new AtomicInteger();
        private final AtomicInteger transactionRollbackCalls = new AtomicInteger();
        private final RTransaction transactionProxy = newProxy(RTransaction.class, (p, method, args) -> {
            if ("commit".equals(method.getName())) {
                transactionCommitCalls.incrementAndGet();
                return null;
            }
            if ("rollback".equals(method.getName())) {
                transactionRollbackCalls.incrementAndGet();
                return null;
            }
            return simpleInvoke(p, method, args);
        });
        private final AtomicReference<String> lastLockName = new AtomicReference<>();
        private final AtomicReference<String> lastRateLimiterName = new AtomicReference<>();
        private final AtomicReference<String> lastQueueName = new AtomicReference<>();
        private final AtomicReference<String> lastLocalMapName = new AtomicReference<>();
        private final AtomicReference<String> lastIdGeneratorName = new AtomicReference<>();
        private final AtomicReference<String> lastBloomFilterName = new AtomicReference<>();
        private final AtomicReference<String> lastExecutorServiceName = new AtomicReference<>();
        private final AtomicReference<String> lastRemoteServiceName = new AtomicReference<>();
        private final AtomicReference<Object> lastLocalMapOptions = new AtomicReference<>();
        private final AtomicReference<Object> lastExecutorOptions = new AtomicReference<>();
        private final AtomicReference<Object> lastPlainOptions = new AtomicReference<>();
        private final AtomicReference<Object> lastOptionalOptions = new AtomicReference<>();
        private final AtomicInteger batchCalls = new AtomicInteger();
        private final AtomicInteger scriptCalls = new AtomicInteger();
        private final AtomicInteger transactionCalls = new AtomicInteger();
        private final AtomicInteger localMapOptionsOnlyCalls = new AtomicInteger();
        private final AtomicInteger localMapLegacyCalls = new AtomicInteger();
        private final AtomicInteger executorServiceNameCalls = new AtomicInteger();
        private final AtomicInteger executorServiceOptionsCalls = new AtomicInteger();
        private final AtomicInteger remoteServiceNameCalls = new AtomicInteger();
        private final AtomicInteger remoteServiceOptionsCalls = new AtomicInteger();
        private final AtomicInteger searchDefaultCalls = new AtomicInteger();
        private final AtomicInteger searchOptionsCalls = new AtomicInteger();
        private final AtomicInteger delayedDestroyCalls = new AtomicInteger();
        private final RDelayedQueue<Object> delayedQueueProxy = newProxy(RDelayedQueue.class, (p, method, args) -> {
            if ("destroy".equals(method.getName())) {
                delayedDestroyCalls.incrementAndGet();
                return null;
            }
            return simpleInvoke(p, method, args);
        });
        private final LockStub lock;

        private ClientStub(LockStub lock) {
            this.lock = lock;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getLock".equals(name)) {
                lastLockName.set(String.valueOf(args[0]));
                return lock.proxy;
            }
            if ("getRateLimiter".equals(name)) {
                lastRateLimiterName.set(String.valueOf(args[0]));
                return rateLimiterProxy;
            }
            if ("getQueue".equals(name)) {
                lastQueueName.set(String.valueOf(args[0]));
                return queueProxy;
            }
            if ("getDelayedQueue".equals(name)) {
                return delayedQueueProxy;
            }
            if ("getLocalCachedMap".equals(name)) {
                if (args != null && args.length == 1) {
                    localMapOptionsOnlyCalls.incrementAndGet();
                    lastLocalMapOptions.set(args[0]);
                } else if (args != null && args.length >= 2) {
                    localMapLegacyCalls.incrementAndGet();
                    lastLocalMapName.set(String.valueOf(args[0]));
                    lastLocalMapOptions.set(args[1]);
                }
                return localCachedMapProxy;
            }
            if ("createBatch".equals(name)) {
                batchCalls.incrementAndGet();
                return batchProxy;
            }
            if ("getExecutorService".equals(name)) {
                if (args != null && args.length == 1 && args[0] instanceof String text) {
                    executorServiceNameCalls.incrementAndGet();
                    lastExecutorServiceName.set(text);
                } else if (args != null && args.length == 1) {
                    executorServiceOptionsCalls.incrementAndGet();
                    lastExecutorOptions.set(args[0]);
                }
                return executorServiceProxy;
            }
            if ("getScript".equals(name)) {
                scriptCalls.incrementAndGet();
                return scriptProxy;
            }
            if ("getIdGenerator".equals(name)) {
                lastIdGeneratorName.set(String.valueOf(args[0]));
                return idGeneratorProxy;
            }
            if ("getBloomFilter".equals(name)) {
                lastBloomFilterName.set(String.valueOf(args[0]));
                return bloomFilterProxy;
            }
            if ("getRemoteService".equals(name)) {
                if (args != null && args.length == 1 && args[0] instanceof String text) {
                    remoteServiceNameCalls.incrementAndGet();
                    lastRemoteServiceName.set(text);
                } else if (args != null && args.length == 1) {
                    remoteServiceOptionsCalls.incrementAndGet();
                    lastPlainOptions.set(args[0]);
                }
                return remoteServiceProxy;
            }
            if ("getSearch".equals(name)) {
                if (args == null || args.length == 0) {
                    searchDefaultCalls.incrementAndGet();
                } else {
                    searchOptionsCalls.incrementAndGet();
                    lastOptionalOptions.set(args[0]);
                }
                return searchProxy;
            }
            if ("createTransaction".equals(name)) {
                transactionCalls.incrementAndGet();
                return transactionProxy;
            }
            if ("toString".equals(name)) {
                return "ClientStub";
            }
            return defaultValue(method.getReturnType());
        }

        private Object simpleInvoke(Object proxy, Method method, Object[] args) {
            if ("toString".equals(method.getName())) {
                return method.getDeclaringClass().getSimpleName() + "Stub";
            }
            return defaultValue(method.getReturnType());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T newProxy(Class<?> api, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                RedissonServiceInterfaceBehaviorTest.class.getClassLoader(),
                new Class<?>[]{api},
                handler
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Byte.TYPE) {
            return (byte) 0;
        }
        if (type == Short.TYPE) {
            return (short) 0;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Float.TYPE) {
            return 0.0f;
        }
        if (type == Double.TYPE) {
            return 0.0d;
        }
        if (type == Character.TYPE) {
            return '\0';
        }
        return null;
    }
}
