package com.monstercontroller.bukkitspring.api.kafka.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KafkaListener {
    String[] topics();

    String groupId();

    String id() default "";

    boolean autoStartup() default true;

    int concurrency() default 1;

    String[] properties() default {};
}
