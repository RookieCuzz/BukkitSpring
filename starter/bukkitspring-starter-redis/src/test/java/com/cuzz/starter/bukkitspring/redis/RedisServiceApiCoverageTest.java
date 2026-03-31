package com.cuzz.starter.bukkitspring.redis;

import com.cuzz.starter.bukkitspring.redis.api.RedisService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RedisServiceApiCoverageTest {

    @Test
    public void shouldExposeQueuePubSubAndStreamApis() {
        Set<String> methods = Set.of(RedisService.class.getMethods()).stream()
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(methods.contains("blpop"));
        assertTrue(methods.contains("brpop"));

        assertTrue(methods.contains("publish"));
        assertTrue(methods.contains("subscribe"));
        assertTrue(methods.contains("psubscribe"));
        assertTrue(methods.contains("subscribeAsync"));
        assertTrue(methods.contains("psubscribeAsync"));

        assertTrue(methods.contains("xadd"));
        assertTrue(methods.contains("xlen"));
        assertTrue(methods.contains("xrange"));
        assertTrue(methods.contains("xrevrange"));
        assertTrue(methods.contains("xdel"));
        assertTrue(methods.contains("xgroupCreate"));
        assertTrue(methods.contains("xgroupDestroy"));
        assertTrue(methods.contains("xgroupCreateConsumer"));
        assertTrue(methods.contains("xgroupDelConsumer"));
        assertTrue(methods.contains("xread"));
        assertTrue(methods.contains("xreadAsMap"));
        assertTrue(methods.contains("xreadGroup"));
        assertTrue(methods.contains("xreadGroupAsMap"));
        assertTrue(methods.contains("xack"));
    }
}
