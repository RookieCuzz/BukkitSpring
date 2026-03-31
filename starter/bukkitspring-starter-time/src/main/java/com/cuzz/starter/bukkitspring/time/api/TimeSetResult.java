package com.cuzz.starter.bukkitspring.time.api;

import java.time.Instant;

/**
 * Result for system time adjustment.
 */
public final class TimeSetResult {
    public final boolean success;
    public final String message;
    public final int exitCode;
    public final String command;
    public final String stdout;
    public final String stderr;
    public final Instant target;

    private TimeSetResult(boolean success,
                          String message,
                          int exitCode,
                          String command,
                          String stdout,
                          String stderr,
                          Instant target) {
        this.success = success;
        this.message = message;
        this.exitCode = exitCode;
        this.command = command;
        this.stdout = stdout;
        this.stderr = stderr;
        this.target = target;
    }

    public static TimeSetResult success(String message,
                                        int exitCode,
                                        String command,
                                        String stdout,
                                        String stderr,
                                        Instant target) {
        return new TimeSetResult(true, message, exitCode, command, stdout, stderr, target);
    }

    public static TimeSetResult failure(String message,
                                        int exitCode,
                                        String command,
                                        String stdout,
                                        String stderr,
                                        Instant target) {
        return new TimeSetResult(false, message, exitCode, command, stdout, stderr, target);
    }
}
