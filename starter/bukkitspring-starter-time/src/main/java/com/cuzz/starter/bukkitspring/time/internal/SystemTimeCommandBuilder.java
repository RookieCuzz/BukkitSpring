package com.cuzz.starter.bukkitspring.time.internal;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class SystemTimeCommandBuilder {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private SystemTimeCommandBuilder() {
    }

    static List<List<String>> buildCommands(Instant target, ZoneId zoneId, boolean preferTimedatectl) {
        return buildCommands(target, zoneId, preferTimedatectl, System.getProperty("os.name", "unknown"));
    }

    static List<List<String>> buildCommands(Instant target, ZoneId zoneId, boolean preferTimedatectl, String osName) {
        if (target == null) {
            return Collections.emptyList();
        }
        ZoneId zone = zoneId == null ? ZoneId.systemDefault() : zoneId;
        String dateTime = DATE_TIME_FORMATTER.format(target.atZone(zone));
        String normalizedOs = osName == null ? "" : osName.toLowerCase(Locale.ROOT);

        if (normalizedOs.contains("win")) {
            String script = "$dt=[datetime]::ParseExact('" + dateTime
                    + "','yyyy-MM-dd HH:mm:ss',[System.Globalization.CultureInfo]::InvariantCulture); Set-Date -Date $dt";
            return List.of(List.of("powershell", "-NoProfile", "-NonInteractive", "-Command", script));
        }

        List<List<String>> commands = new ArrayList<>();
        List<String> timedatectl = List.of("timedatectl", "set-time", dateTime);
        List<String> date = List.of("date", "-s", dateTime);

        if (preferTimedatectl) {
            commands.add(timedatectl);
            commands.add(date);
        } else {
            commands.add(date);
            commands.add(timedatectl);
        }
        return commands;
    }
}
