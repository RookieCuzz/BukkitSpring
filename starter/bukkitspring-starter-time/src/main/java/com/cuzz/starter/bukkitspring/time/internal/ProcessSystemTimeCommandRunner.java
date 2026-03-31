package com.cuzz.starter.bukkitspring.time.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class ProcessSystemTimeCommandRunner implements SystemTimeCommandRunner {
    @Override
    public CommandResult run(List<String> command, long timeoutMillis) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();
        boolean finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new CommandResult(-1, read(process.getInputStream()), "Process timeout", true);
        }
        int exitCode = process.exitValue();
        String stdout = read(process.getInputStream());
        String stderr = read(process.getErrorStream());
        return new CommandResult(exitCode, stdout, stderr, false);
    }

    private String read(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
