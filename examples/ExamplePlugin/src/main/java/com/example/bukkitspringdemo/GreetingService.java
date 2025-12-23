package com.example.bukkitspringdemo;

import com.monstercontroller.bukkitspring.api.annotation.Component;

@Component
public final class GreetingService {
    public String greet(String name) {
        return "Hello " + name + "!";
    }
}
