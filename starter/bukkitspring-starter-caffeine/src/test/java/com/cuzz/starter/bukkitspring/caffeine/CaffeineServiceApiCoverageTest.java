package com.cuzz.starter.bukkitspring.caffeine;

import com.cuzz.starter.bukkitspring.caffeine.api.CaffeineService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaffeineServiceApiCoverageTest {

    @Test
    public void shouldExposeCoreCacheAsyncAndLoadingApis() {
        Set<String> methods = Set.of(CaffeineService.class.getMethods()).stream()
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(methods.contains("newBuilder"));
        assertTrue(methods.contains("typedBuilder"));
        assertTrue(methods.contains("getCache"));
        assertTrue(methods.contains("typedCache"));
        assertTrue(methods.contains("getAsyncCache"));
        assertTrue(methods.contains("typedAsyncCache"));
        assertTrue(methods.contains("getLoadingCache"));
        assertTrue(methods.contains("typedLoadingCache"));
        assertTrue(methods.contains("getAsyncLoadingCache"));
        assertTrue(methods.contains("typedAsyncLoadingCache"));
        assertTrue(methods.contains("typedPolicy"));
        assertTrue(methods.contains("typedGetIfPresent"));
        assertTrue(methods.contains("typedGet"));
        assertTrue(methods.contains("typedPut"));
        assertTrue(methods.contains("typedAsyncGet"));
        assertTrue(methods.contains("typedLoadingGet"));
        assertTrue(methods.contains("typedAsyncLoadingGet"));

        assertTrue(methods.contains("getAllPresent"));
        assertTrue(methods.contains("getAll"));
        assertTrue(methods.contains("cleanUp"));
        assertTrue(methods.contains("stats"));
        assertTrue(methods.contains("policy"));
        assertTrue(methods.contains("evictionPolicy"));
        assertTrue(methods.contains("expireAfterAccessPolicy"));
        assertTrue(methods.contains("expireAfterWritePolicy"));
        assertTrue(methods.contains("variableExpirationPolicy"));
        assertTrue(methods.contains("refreshAfterWritePolicy"));
        assertTrue(methods.contains("refreshes"));

        assertTrue(methods.contains("asyncGetIfPresent"));
        assertTrue(methods.contains("asyncGet"));
        assertTrue(methods.contains("asyncGetAll"));
        assertTrue(methods.contains("asyncPut"));
        assertTrue(methods.contains("asyncAsMap"));
        assertTrue(methods.contains("synchronous"));

        assertTrue(methods.contains("loadingGet"));
        assertTrue(methods.contains("loadingGetAll"));
        assertTrue(methods.contains("loadingRefresh"));
        assertTrue(methods.contains("loadingRefreshAll"));
        assertTrue(methods.contains("asyncLoadingGet"));
        assertTrue(methods.contains("asyncLoadingGetAll"));
        assertTrue(methods.contains("asyncLoadingSynchronous"));
    }
}
