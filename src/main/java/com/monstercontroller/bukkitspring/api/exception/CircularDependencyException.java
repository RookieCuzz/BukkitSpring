package com.monstercontroller.bukkitspring.api.exception;

public class CircularDependencyException extends RuntimeException {
    public CircularDependencyException(String message) {
        super(message);
    }
}
