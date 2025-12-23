package com.monstercontroller.bukkitspring.internal;

final class BeanNames {
    private BeanNames() {
    }

    static String defaultName(Class<?> type) {
        String simple = type.getSimpleName();
        if (simple.isEmpty()) {
            return type.getName();
        }
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }
}
