package com.example.bukkitspringvalidate;

import com.monstercontroller.bukkitspring.api.annotation.Component;
import com.monstercontroller.bukkitspring.api.annotation.Scope;
import com.monstercontroller.bukkitspring.api.annotation.ScopeType;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Scope(ScopeType.PROTOTYPE)
public final class TempObject {
    private static final AtomicInteger COUNTER = new AtomicInteger();
    private final int id = COUNTER.incrementAndGet();

    public int getId() {
        return id;
    }
}
