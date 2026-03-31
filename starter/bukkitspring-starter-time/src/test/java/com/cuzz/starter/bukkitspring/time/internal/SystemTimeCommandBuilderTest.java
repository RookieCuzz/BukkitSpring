package com.cuzz.starter.bukkitspring.time.internal;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SystemTimeCommandBuilderTest {

    @Test
    public void windowsCommandUsesPowerShellSetDate() {
        List<List<String>> commands = SystemTimeCommandBuilder.buildCommands(
                Instant.parse("2026-01-02T03:04:05Z"),
                ZoneId.of("UTC"),
                true,
                "Windows Server 2022"
        );

        assertEquals(1, commands.size());
        List<String> command = commands.get(0);
        assertEquals("powershell", command.get(0));
        assertTrue(command.get(command.size() - 1).contains("Set-Date"));
    }

    @Test
    public void linuxCommandOrderRespectsPreference() {
        List<List<String>> commandsPrefer = SystemTimeCommandBuilder.buildCommands(
                Instant.parse("2026-01-02T03:04:05Z"),
                ZoneId.of("UTC"),
                true,
                "Linux"
        );
        List<List<String>> commandsNotPrefer = SystemTimeCommandBuilder.buildCommands(
                Instant.parse("2026-01-02T03:04:05Z"),
                ZoneId.of("UTC"),
                false,
                "Linux"
        );

        assertFalse(commandsPrefer.isEmpty());
        assertFalse(commandsNotPrefer.isEmpty());
        assertEquals("timedatectl", commandsPrefer.get(0).get(0));
        assertEquals("date", commandsNotPrefer.get(0).get(0));
    }
}
