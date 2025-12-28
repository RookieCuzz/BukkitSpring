package com.monstercontroller.bukkitspring.kafka;

import com.monstercontroller.bukkitspring.api.kafka.ConsumerRegistration;
import com.monstercontroller.bukkitspring.api.kafka.KafkaConsumerManager;
import com.monstercontroller.bukkitspring.api.kafka.KafkaMessageHandler;
import com.monstercontroller.bukkitspring.api.kafka.annotation.KafkaListener;
import com.monstercontroller.bukkitspring.internal.SimpleApplicationContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class KafkaListenerProcessor {
    private final KafkaConsumerManager manager;
    private final Logger logger;

    public KafkaListenerProcessor(KafkaConsumerManager manager, Logger logger) {
        this.manager = manager;
        this.logger = logger;
    }

    public void process(SimpleApplicationContext context) {
        if (manager == null) {
            logger.warning("[KafkaListener] KafkaConsumerManager is null, skipping listener processing");
            return;
        }
        
        logger.info("[KafkaListener] Starting to process @KafkaListener annotations");
        int listenerCount = 0;
        
        // 直接遍历所有已注册的 Bean，避免 classgraph 的 ClassLoader 冲突
        List<Object> allBeans = context.getAllBeans();
        logger.info("[KafkaListener] Found " + allBeans.size() + " beans to scan");
        
        for (Object bean : allBeans) {
            Class<?> type = bean.getClass();
            
            // 扫描该类的所有方法
            for (Method method : type.getDeclaredMethods()) {
                KafkaListener listener = method.getAnnotation(KafkaListener.class);
                if (listener == null) {
                    continue;
                }
                
                logger.info("[KafkaListener] Found @KafkaListener in class: " + type.getName());
                logger.info("[KafkaListener] Registering listener method: " + method.getName() + 
                           " for topics: " + String.join(", ", listener.topics()));
                
                if (!isValidSignature(method)) {
                    logger.warning("KafkaListener method must accept a ConsumerRecord: " + method);
                    continue;
                }
                
                method.setAccessible(true);
                registerListener(bean, method, listener);
                listenerCount++;
            }
        }
        
        logger.info("[KafkaListener] Total listeners registered: " + listenerCount);
    }

    private boolean isValidSignature(Method method) {
        Class<?>[] params = method.getParameterTypes();
        return params.length == 1 && ConsumerRecord.class.isAssignableFrom(params[0]);
    }

    private void registerListener(Object bean, Method method, KafkaListener listener) {
        try {
            KafkaMessageHandler<Object, Object> handler = record -> invoke(bean, method, record);
            Map<String, Object> props = parseProperties(listener.properties());
            
            // 添加默认的反序列化器（如果用户没有指定）
            props.putIfAbsent("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.putIfAbsent("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            
            ConsumerRegistration<Object, Object> registration = ConsumerRegistration.builder()
                    .id(listener.id())
                    .topics(listener.topics())
                    .groupId(listener.groupId())
                    .handler(handler)
                    .concurrency(listener.concurrency())
                    .autoStartup(listener.autoStartup())
                    .properties(props)
                    .build();
            String consumerId = manager.registerConsumer(registration);
            logger.info(String.format("[KafkaListener] Registered consumer [id=%s, topics=%s, groupId=%s]",
                    consumerId, String.join(", ", listener.topics()), listener.groupId()));
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "KafkaListener registration failed for method: " + method, ex);
        }
    }

    private void invoke(Object bean, Method method, ConsumerRecord<?, ?> record) throws Exception {
        try {
            method.invoke(bean, record);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new Exception(cause);
        } catch (IllegalAccessException ex) {
            throw new Exception(ex);
        }
    }

    private Map<String, Object> parseProperties(String[] entries) {
        Map<String, Object> props = new LinkedHashMap<>();
        if (entries == null || entries.length == 0) {
            return props;
        }
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            int idx = entry.indexOf('=');
            if (idx <= 0) {
                logger.warning("KafkaListener property ignored: " + entry);
                continue;
            }
            String key = entry.substring(0, idx).trim();
            String value = entry.substring(idx + 1).trim();
            if (!key.isEmpty()) {
                props.put(key, value);
            }
        }
        return props;
    }
}
