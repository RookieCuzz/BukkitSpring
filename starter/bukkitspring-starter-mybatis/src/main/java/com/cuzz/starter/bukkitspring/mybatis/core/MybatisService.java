package com.cuzz.starter.bukkitspring.mybatis.core;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.function.Consumer;
import java.util.function.Function;
public interface MybatisService {
    boolean isEnabled();

    boolean isAutoCommit();

    SqlSession openSession(boolean autoCommit);

    SqlSessionFactory getSqlSessionFactory();

    <T> T withSession(Function<SqlSession, T> work);

    void withSession(Consumer<SqlSession> work);

    void registerMapper(Object pluginKey, String resourcePath);

    void registerMapper(String pluginPackage, String resourcePath);
}
