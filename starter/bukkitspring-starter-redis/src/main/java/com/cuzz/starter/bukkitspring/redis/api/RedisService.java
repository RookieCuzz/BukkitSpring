package com.cuzz.starter.bukkitspring.redis.api;

import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.params.BitPosParams;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;

import java.util.Map.Entry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Redis facade service.
 *
 * <p>Provides a unified client plus common operations for string, hash, list,
 * set, zset, and bitmap data types.
 */
public interface RedisService extends AutoCloseable {
    boolean isEnabled();

    RedisMode mode();

    ExecutorService executor();

    UnifiedJedis client();

    default boolean isCluster() {
        return mode() == RedisMode.CLUSTER;
    }

    default CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor());
    }

    default <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor());
    }

    // -------------------- String operations --------------------

    default String get(String key) {
        return client().get(key);
    }

    default String set(String key, String value) {
        return client().set(key, value);
    }

    default String setEx(String key, long seconds, String value) {
        return client().setex(key, seconds, value);
    }

    default long del(String... keys) {
        return client().del(keys);
    }

    default boolean exists(String key) {
        return client().exists(key);
    }

    default long expire(String key, long seconds) {
        return client().expire(key, seconds);
    }

    default long ttl(String key) {
        return client().ttl(key);
    }

    default long incr(String key) {
        return client().incr(key);
    }

    default long incrBy(String key, long delta) {
        return client().incrBy(key, delta);
    }

    default long decr(String key) {
        return client().decr(key);
    }

    // -------------------- Hash operations --------------------

    default long hset(String key, String field, String value) {
        return client().hset(key, field, value);
    }

    default long hset(String key, Map<String, String> values) {
        return client().hset(key, values);
    }

    default String hget(String key, String field) {
        return client().hget(key, field);
    }

    default Map<String, String> hgetAll(String key) {
        return client().hgetAll(key);
    }

    default boolean hexists(String key, String field) {
        return client().hexists(key, field);
    }

    default long hdel(String key, String... fields) {
        return client().hdel(key, fields);
    }

    default long hincrBy(String key, String field, long delta) {
        return client().hincrBy(key, field, delta);
    }

    // -------------------- List operations --------------------

    default long lpush(String key, String... values) {
        return client().lpush(key, values);
    }

    default long rpush(String key, String... values) {
        return client().rpush(key, values);
    }

    default List<String> lrange(String key, long start, long stop) {
        return client().lrange(key, start, stop);
    }

    default String lpop(String key) {
        return client().lpop(key);
    }

    default String rpop(String key) {
        return client().rpop(key);
    }

    default long llen(String key) {
        return client().llen(key);
    }

    default List<String> blpop(int timeoutSeconds, String... keys) {
        return client().blpop(timeoutSeconds, keys);
    }

    default List<String> brpop(int timeoutSeconds, String... keys) {
        return client().brpop(timeoutSeconds, keys);
    }

    // -------------------- Set operations --------------------

    default long sadd(String key, String... members) {
        return client().sadd(key, members);
    }

    default long srem(String key, String... members) {
        return client().srem(key, members);
    }

    default Set<String> smembers(String key) {
        return client().smembers(key);
    }

    default boolean sismember(String key, String member) {
        return client().sismember(key, member);
    }

    default long scard(String key) {
        return client().scard(key);
    }

    // -------------------- ZSet operations --------------------

    default long zadd(String key, double score, String member) {
        return client().zadd(key, score, member);
    }

    default long zrem(String key, String... members) {
        return client().zrem(key, members);
    }

    default List<String> zrange(String key, long start, long stop) {
        return client().zrange(key, start, stop);
    }

    default List<String> zrevrange(String key, long start, long stop) {
        return client().zrevrange(key, start, stop);
    }

    default Double zscore(String key, String member) {
        return client().zscore(key, member);
    }

    // -------------------- Bitmap operations --------------------

    default boolean setBit(String key, long offset, boolean value) {
        return client().setbit(key, offset, value);
    }

    default boolean getBit(String key, long offset) {
        return client().getbit(key, offset);
    }

    default long bitCount(String key) {
        return client().bitcount(key);
    }

    default long bitCount(String key, long start, long end) {
        return client().bitcount(key, start, end);
    }

    default long bitPos(String key, boolean value) {
        return client().bitpos(key, value);
    }

    default long bitPos(String key, boolean value, BitPosParams params) {
        return client().bitpos(key, value, params);
    }

    default long bitOp(BitOP operation, String destKey, String... keys) {
        return client().bitop(operation, destKey, keys);
    }

    // -------------------- Pub/Sub operations --------------------

    default long publish(String channel, String message) {
        return client().publish(channel, message);
    }

    /**
     * Subscribe is a blocking call and should usually run in async context.
     */
    default void subscribe(JedisPubSub listener, String... channels) {
        client().subscribe(listener, channels);
    }

    /**
     * Pattern subscribe is a blocking call and should usually run in async context.
     */
    default void psubscribe(JedisPubSub listener, String... patterns) {
        client().psubscribe(listener, patterns);
    }

    default CompletableFuture<Void> subscribeAsync(JedisPubSub listener, String... channels) {
        return runAsync(() -> subscribe(listener, channels));
    }

    default CompletableFuture<Void> psubscribeAsync(JedisPubSub listener, String... patterns) {
        return runAsync(() -> psubscribe(listener, patterns));
    }

    // -------------------- Stream operations --------------------

    default StreamEntryID xadd(String key, Map<String, String> fields) {
        return client().xadd(key, StreamEntryID.NEW_ENTRY, fields);
    }

    default StreamEntryID xadd(String key, XAddParams params, Map<String, String> fields) {
        return client().xadd(key, params, fields);
    }

    default StreamEntryID xadd(String key, StreamEntryID entryId, Map<String, String> fields) {
        return client().xadd(key, entryId, fields);
    }

    default long xlen(String key) {
        return client().xlen(key);
    }

    default List<StreamEntry> xrange(String key, String start, String end) {
        return client().xrange(key, start, end);
    }

    default List<StreamEntry> xrange(String key, String start, String end, int count) {
        return client().xrange(key, start, end, count);
    }

    default List<StreamEntry> xrevrange(String key, String end, String start) {
        return client().xrevrange(key, end, start);
    }

    default List<StreamEntry> xrevrange(String key, String end, String start, int count) {
        return client().xrevrange(key, end, start, count);
    }

    default long xdel(String key, StreamEntryID... ids) {
        return client().xdel(key, ids);
    }

    default String xgroupCreate(String key, String group, StreamEntryID id, boolean mkStream) {
        return client().xgroupCreate(key, group, id, mkStream);
    }

    default long xgroupDestroy(String key, String group) {
        return client().xgroupDestroy(key, group);
    }

    default boolean xgroupCreateConsumer(String key, String group, String consumer) {
        return client().xgroupCreateConsumer(key, group, consumer);
    }

    default long xgroupDelConsumer(String key, String group, String consumer) {
        return client().xgroupDelConsumer(key, group, consumer);
    }

    default List<Entry<String, List<StreamEntry>>> xread(XReadParams params, Map<String, StreamEntryID> streams) {
        return client().xread(params, streams);
    }

    default Map<String, List<StreamEntry>> xreadAsMap(XReadParams params, Map<String, StreamEntryID> streams) {
        return client().xreadAsMap(params, streams);
    }

    default List<Entry<String, List<StreamEntry>>> xreadGroup(String group,
                                                              String consumer,
                                                              XReadGroupParams params,
                                                              Map<String, StreamEntryID> streams) {
        return client().xreadGroup(group, consumer, params, streams);
    }

    default Map<String, List<StreamEntry>> xreadGroupAsMap(String group,
                                                            String consumer,
                                                            XReadGroupParams params,
                                                            Map<String, StreamEntryID> streams) {
        return client().xreadGroupAsMap(group, consumer, params, streams);
    }

    default long xack(String key, String group, StreamEntryID... ids) {
        return client().xack(key, group, ids);
    }

    @Override
    void close();
}
