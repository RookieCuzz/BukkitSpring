package com.cuzz.starter.bukkitspring.time.internal;

import java.io.IOException;
import java.util.List;

interface SystemTimeCommandRunner {
    CommandResult run(List<String> command, long timeoutMillis) throws IOException, InterruptedException;

    final class CommandResult {
        final int exitCode;
        final String stdout;
        final String stderr;
        final boolean timedOut;

        CommandResult(int exitCode, String stdout, String stderr, boolean timedOut) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.timedOut = timedOut;
        }
    }
}
