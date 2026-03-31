# 三级缓存 + AOP 早期暴露设计文档

## 背景
当前 `SimpleApplicationContext` 只有两级缓存（`singletons`、`earlySingletons`）和创建中集合，能解决一部分循环依赖，但无法在“早期引用”阶段暴露代理，也无法保证 AOP 代理的一致性。

本设计引入 Spring 风格的三级缓存机制，并加入 AOP 扩展点，使循环依赖场景下也能提前暴露代理，且最终引用一致。

## 目标
- 解决单例的字段/方法注入循环依赖。
- 支持 AOP 代理的提前暴露。
- 保证早期引用和最终引用一致。
- 在现有 `SimpleApplicationContext` 上小幅改造即可接入。

## 非目标
- 不支持原型作用域的循环依赖。
- 不追求完整 Spring BeanFactory 功能。
- 不实现字节码织入，只支持代理式 AOP。

## 缓存机制（三级缓存）
- L1 `singletonObjects`：完全初始化后的单例。
- L2 `earlySingletonObjects`：早期引用，可能是代理。
- L3 `singletonFactories`：用于创建早期引用的工厂（允许 AOP 介入）。
- `singletonsCurrentlyInCreation`：创建中的 bean 名称集合。

## 数据结构
```java
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();
private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();
private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet();
```

`ObjectFactory<T>`（最小定义）：
```java
@FunctionalInterface
public interface ObjectFactory<T> {
    T getObject();
}
```

## AOP 扩展点
增加最小后置处理器接口，支持“早期代理”和“最终代理”：
```java
public interface BeanPostProcessor {
    Object postProcessBeforeInitialization(Object bean, String name);
    Object postProcessAfterInitialization(Object bean, String name);
}

public interface SmartInstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    Object getEarlyBeanReference(Object bean, String name);
}
```
说明：
- `getEarlyBeanReference` 用于 L3 工厂生成早期引用（可返回代理）。
- `postProcessAfterInitialization` 用于最终代理包装。

## 核心流程

### 1) getSingleton(name, allowEarlyReference)
```
if (singletonObjects contains name) return it
if (allowEarlyReference && isCurrentlyInCreation(name)) {
    if (earlySingletonObjects contains name) return it
    if (singletonFactories contains name) {
        early = singletonFactories.get(name).getObject()
        earlySingletonObjects.put(name, early)
        singletonFactories.remove(name)
        return early
    }
}
return null
```

### 2) createBean(name)
```
mark name in creation
instance = instantiate(name)
add singletonFactories[name] = () -> getEarlyBeanReference(name, instance)
populateProperties(instance)
initializeBean(instance)
exposed = postProcessAfterInitialization(instance)
if (earlySingletonObjects contains name) {
    // 已暴露早期引用时，优先保持一致
    exposed = earlySingletonObjects.get(name)
}
move exposed to singletonObjects
cleanup earlySingletonObjects/singletonFactories/creation markers
return exposed
```

### 3) getEarlyBeanReference(name, instance)
```
Object result = instance
for each SmartInstantiationAwareBeanPostProcessor:
    result = p.getEarlyBeanReference(result, name)
return result
```
这样 AOP 能在属性注入完成前返回代理。

## 一致性规则
- 早期返回的代理必须成为最终单例引用。
- 如果早期引用已被注入，最终阶段不得替换为不同对象。

## 循环依赖行为
- 单例 + 字段/方法注入：可通过三级缓存解决。
- 构造器循环依赖：仍会失败（实例化阶段无法完成）。
- 原型作用域循环依赖：直接抛出 `CircularDependencyException`。

## 线程安全
- 缓存使用 `ConcurrentHashMap`。
- 创建流程使用 `synchronized` 或独立锁避免重复创建。
- `singletonsCurrentlyInCreation` 必须在 `try/finally` 中维护。

## 集成计划（SimpleApplicationContext）
- 替换现有 `singletons`、`earlySingletons` 为三级缓存结构。
- 新增 `singletonFactories` 与 `getSingleton(name, allowEarlyReference)`。
- 在 `createBean` 中实例化后注册工厂，注入前完成早期暴露。
- 增加后置处理器集合，接入 AOP 扩展点。

## 测试计划
- 字段注入 A <-> B 循环依赖：无 AOP，应成功。
- 同场景 + AOP：B 注入到的是 A 的代理，最终 A 也应是同一代理。
- 构造器循环依赖：应抛出异常。
- 原型循环依赖：应抛出异常。

## 流程图
```mermaid
flowchart TD
    A[resolveBean(name)] --> B{singletonObjects 中存在?}
    B -- 是 --> R[返回 singletonObjects[name]]
    B -- 否 --> C{正在创建中?}
    C -- 否 --> D[createBean(name)]
    C -- 是 --> E{允许早期引用?}
    E -- 否 --> F[抛出 CircularDependencyException]
    E -- 是 --> G{earlySingletonObjects 中存在?}
    G -- 是 --> R2[返回 earlySingletonObjects[name]]
    G -- 否 --> H{singletonFactories 中存在?}
    H -- 是 --> I[early = factory.getObject()]
    I --> J[earlySingletonObjects put early]
    J --> K[移除 singletonFactories[name]]
    K --> R3[返回 early]
    H -- 否 --> F

    D --> D1[实例化 bean]
    D1 --> D2[注册 singletonFactory]
    D2 --> D3[属性注入]
    D3 --> D4[初始化]
    D4 --> D5[postProcessAfterInitialization]
    D5 --> D6{earlySingletonObjects 中存在?}
    D6 -- 是 --> D7[exposed = early 引用]
    D6 -- 否 --> D8[exposed = post-processed]
    D7 --> D9[写入 singletonObjects]
    D8 --> D9
    D9 --> D10[清理缓存与创建标记]
    D10 --> R4[返回 exposed]
```
