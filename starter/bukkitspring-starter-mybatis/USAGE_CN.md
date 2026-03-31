# MyBatis Starter 使用文档

## 概述
MyBatis starter 提供：
- `MybatisService`（启用后注册为全局 Bean）
- `SqlSessionFactory`（启用后注册为全局 Bean）
- 自动扫描 `@Mapper` 接口与 `mappers/*.xml` 映射文件

## 安装
1. 将 `bukkitspring-starter-mybatis-*.jar` 放入 `plugins/BukkitSpring/starters/`。
2. 重启服务器。

## 配置
编辑 `plugins/BukkitSpring/config.yml`。

```yaml
mybatis:
  enabled: true
  debug: false # true 时输出 MyBatis SQL 详细日志
  auto-commit: true
  jdbc-url: "jdbc:mysql://127.0.0.1:3306/demo"
  username: "root"
  password: "password"
  driver: "com.mysql.cj.jdbc.Driver"
  pool:
    max-size: 10
    min-idle: 5
    connection-timeout-ms: 30000
    max-lifetime-ms: 1800000
    idle-timeout-ms: 600000
    keepalive-time-ms: 0
    validation-timeout-ms: 5000
    leak-detection-threshold-ms: 0
  log-impl: "JDK_LOGGING" # 仅在 debug=true 时生效，STDOUT_LOGGING 会输出大量控制台日志
  slow-sql-threshold-ms: 0 # 0 表示关闭慢 SQL 日志
```

## Mapper 注册规则
1. **注解 Mapper**：接口必须标注 `@Mapper`。
2. **XML Mapper**：放到 `src/main/resources/mappers/*.xml`。
   - `namespace` 必须是接口的全限定名。
   - 启动时会自动提取到 `plugins/<你的插件>/mappers/` 并注册。
3. 扫描包来自 `BukkitSpring.registerPlugin(..., "com.example.pkg")`
   的包名（或回退到插件主包）。

## 使用方式
### Mapper 接口（`@Mapper`）
```java
@Mapper
public interface UserMapper {
  User selectById(@Param("id") int id);
}
```

### 注入 Mapper 接口
当 MyBatis 启用且接口标注 `@Mapper` 时，Mapper 会作为 Bean 注册并可注入。
```java
@Component
public final class UserService {
  @Autowired(required = false)
  private UserMapper userMapper;

  public void run() {
    if (userMapper == null) {
      return;
    }
    User user = userMapper.selectById(1);
  }
}
```

### 混合 XML + 注解 CRUD（含事务）
Mapper 接口（`@Mapper` + 注解）：
```java
@Mapper
public interface UserMapper {
  @Select("select id, name, age from bstest_users_int where id = #{id}")
  User selectById(@Param("id") int id);

  @Insert("insert into bstest_users_int(name, age) values(#{name}, #{age})")
  int insert(User user);

  @Update("update bstest_users_int set name = #{name} where id = #{id}")
  int updateName(@Param("id") int id, @Param("name") String name);

  @Delete("delete from bstest_users_int where id = #{id}")
  int deleteById(@Param("id") int id);

  List<User> selectByFilter(@Param("name") String name, @Param("minAge") Integer minAge);
}
```

XML 映射（`src/main/resources/mappers/UserMapper.xml`）：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.demo.UserMapper">
  <select id="selectByFilter" resultType="com.example.demo.User">
    select id, name, age
    from bstest_users_int
    <where>
      <if test="name != null and name != ''">
        and name like concat('%', #{name}, '%')
      </if>
      <if test="minAge != null">
        and age &gt;= #{minAge}
      </if>
    </where>
    order by id desc
  </select>
</mapper>
```

事务用法（手动提交/回滚）：
```java
public void updateTwoUsers(int idA, int idB) {
  if (mybatis == null || !mybatis.isEnabled()) {
    return;
  }
  SqlSession session = mybatis.openSession(false);
  try {
    UserMapper mapper = session.getMapper(UserMapper.class);
    mapper.updateName(idA, "alpha");
    mapper.updateName(idB, "beta");
    session.commit();
  } catch (Exception ex) {
    session.rollback();
    throw ex;
  } finally {
    session.close();
  }
}
```

### 注入
```java
@Component
public final class MybatisExample {
  @Autowired(required = false)
  private MybatisService mybatis;

  public void run() {
    if (mybatis == null || !mybatis.isEnabled()) {
      return;
    }
    mybatis.withSession(session -> {
      // session.selectOne(...)
    });
  }
}
```

### 直接使用 SqlSessionFactory
```java
SqlSessionFactory factory = BukkitSpring.getGlobalBean(SqlSessionFactory.class);
try (SqlSession session = factory.openSession(true)) {
  // use session
}
```

## 常见问题
- `MyBatis disabled`：`mybatis.enabled=false` 或 `mybatis.jdbc-url` 为空。
- Mapper 找不到：确认 `@Mapper` 注解与 XML `namespace` 是否正确。
