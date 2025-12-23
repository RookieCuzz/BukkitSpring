package com.example.bukkitspringvalidate;

import com.monstercontroller.bukkitspring.api.annotation.Component;
import com.monstercontroller.bukkitspring.api.annotation.Primary;

@Component
@Primary
public final class DefaultStorage implements Storage {
    @Override
    public String name() {
        return "default-storage";
    }
}
