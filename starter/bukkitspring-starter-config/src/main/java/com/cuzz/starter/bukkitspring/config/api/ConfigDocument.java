package com.cuzz.starter.bukkitspring.config.api;

import java.nio.charset.Charset;

/**
 * Loaded config payload metadata.
 */
public record ConfigDocument(
        String name,
        String content,
        String source,
        long loadedAtMillis,
        Charset charset
) {
}
