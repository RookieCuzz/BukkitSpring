# MyBatis Starter (Usage)

## Overview
The MyBatis starter provides:
- `MybatisService` (global bean when enabled)
- `SqlSessionFactory` (global bean when enabled)
- Automatic mapper scanning (`@Mapper`) and XML loading (`mappers/*.xml`)

## Installation
1. Copy `bukkitspring-starter-mybatis-*.jar` to `plugins/BukkitSpring/starters/`.
2. Restart the server.

## Configuration
Edit `plugins/BukkitSpring/config.yml`.

```yaml
mybatis:
  enabled: true
  debug: false # when true, enables MyBatis SQL log output
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
  log-impl: "JDK_LOGGING" # effective only when debug=true; STDOUT_LOGGING is noisy
  slow-sql-threshold-ms: 0 # 0 disables slow SQL logging
```

## Mapper registration rules
1. **Annotation mappers**: interfaces must be annotated with `@Mapper`.
2. **XML mappers**: place files under `src/main/resources/mappers/*.xml`.
   - XML `namespace` must match the full interface name.
   - On startup, XML resources are extracted to `plugins/<YourPlugin>/mappers/`
     and registered automatically.
3. The starter scans packages from your `BukkitSpring.registerPlugin(..., "com.example.pkg")`
   call (or falls back to the plugin main package).

## Usage
### Mapper interface (`@Mapper`)
```java
@Mapper
public interface UserMapper {
  User selectById(@Param("id") int id);
}
```

### Inject mapper interface
When MyBatis is enabled and the interface is annotated with `@Mapper`, the mapper
is registered as a bean and can be injected.
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

### Mixed XML + annotation CRUD (with transaction)
Mapper interface (`@Mapper` + annotations):
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

XML mapper (`src/main/resources/mappers/UserMapper.xml`):
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

Transactional usage (manual commit/rollback):
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

### Injection
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

### Direct SqlSessionFactory
```java
SqlSessionFactory factory = BukkitSpring.getGlobalBean(SqlSessionFactory.class);
try (SqlSession session = factory.openSession(true)) {
  // use session
}
```

## Troubleshooting
- `MyBatis disabled`: `mybatis.enabled=false` or `mybatis.jdbc-url` missing.
- Mapper not found: check `@Mapper` annotation and XML `namespace`.
