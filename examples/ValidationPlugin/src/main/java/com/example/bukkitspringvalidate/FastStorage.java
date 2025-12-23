package com.example.bukkitspringvalidate;

import com.monstercontroller.bukkitspring.api.annotation.Component;

@Component("fastStorage")
public final class FastStorage implements Storage {
    @Override
    public String name() {
        return "fast-storage";
    }
}
