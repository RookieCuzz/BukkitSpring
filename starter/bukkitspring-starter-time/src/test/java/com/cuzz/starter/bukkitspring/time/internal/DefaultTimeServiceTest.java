package com.cuzz.starter.bukkitspring.time.internal;

import com.cuzz.starter.bukkitspring.time.api.TimeSetResult;
import com.cuzz.starter.bukkitspring.time.config.TimeSettings;
import com.cuzz.starter.bukkitspring.time.testutil.MapConfigView;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultTimeServiceTest {

    @Test
    public void setSystemTimeBlockedWhenAllowSetDisabled() {
        FakeRunner runner = new FakeRunner();
        TimeSettings settings = TimeSettings.fromConfig(new MapConfigView(Map.of(
                "time.enabled", true,
                "time.system-time.allow-set", false
        )));
        DefaultTimeService service = new DefaultTimeService(settings, Logger.getLogger("test"), runner, "Linux");

        TimeSetResult result = service.setSystemTime(Instant.parse("2026-01-01T00:00:00Z"));

        assertFalse(result.success);
        assertTrue(result.message.contains("disabled"));
        assertEquals(0, runner.commands.size());
    }

    @Test
    public void setSystemTimeFallsBackToSecondCommand() {
        FakeRunner runner = new FakeRunner();
        runner.results.add(new SystemTimeCommandRunner.CommandResult(1, "", "no timedatectl", false));
        runner.results.add(new SystemTimeCommandRunner.CommandResult(0, "ok", "", false));

        TimeSettings settings = TimeSettings.fromConfig(new MapConfigView(Map.of(
                "time.enabled", true,
                "time.system-time.allow-set", true,
                "time.system-time.prefer-timedatectl", true
        )));
        DefaultTimeService service = new DefaultTimeService(settings, Logger.getLogger("test"), runner, "Linux");

        TimeSetResult result = service.setSystemTime(Instant.parse("2026-01-01T00:00:00Z"));

        assertTrue(result.success);
        assertEquals(2, runner.commands.size());
        assertEquals("timedatectl", runner.commands.get(0).get(0));
        assertEquals("date", runner.commands.get(1).get(0));
    }

    @Test
    public void debugOffsetAffectsCurrentTimeAndAsyncSetReturnsResult() throws Exception {
        FakeRunner runner = new FakeRunner();
        runner.results.add(new SystemTimeCommandRunner.CommandResult(0, "ok", "", false));
        TimeSettings settings = TimeSettings.fromConfig(new MapConfigView(Map.of(
                "time.enabled", true,
                "time.system-time.allow-set", true
        )));
        DefaultTimeService service = new DefaultTimeService(settings, Logger.getLogger("test"), runner, "Linux");

        long before = System.currentTimeMillis();
        service.setDebugOffset(Duration.ofSeconds(2));
        long adjusted = service.currentTimeMillis();
        assertTrue(adjusted >= before + 1500);
        assertEquals(Duration.ofSeconds(2), service.debugOffset());

        TimeSetResult asyncResult = service.setSystemTimeAsync(Instant.parse("2026-01-01T00:00:00Z"))
                .get(3, TimeUnit.SECONDS);
        assertNotNull(asyncResult);
        assertTrue(asyncResult.success);
    }

    private static final class FakeRunner implements SystemTimeCommandRunner {
        private final List<List<String>> commands = new ArrayList<>();
        private final List<CommandResult> results = new ArrayList<>();

        @Override
        public CommandResult run(List<String> command, long timeoutMillis) throws IOException, InterruptedException {
            commands.add(command);
            if (results.isEmpty()) {
                return new CommandResult(0, "", "", false);
            }
            return results.remove(0);
        }
    }
}
