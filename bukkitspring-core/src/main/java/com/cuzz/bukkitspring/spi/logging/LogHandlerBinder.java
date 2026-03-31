package com.cuzz.bukkitspring.spi.logging;

import java.util.logging.Handler;

public interface LogHandlerBinder {
    void attach(Handler handler, LogTarget target);

    void detach(Handler handler);

    default boolean isAvailable() {
        return true;
    }

    default int order() {
        return 0;
    }
}
