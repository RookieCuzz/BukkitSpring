package com.cuzz.starter.bukkitspring.mybatis.internal.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;

public class HikariDataSourceFactory extends PooledDataSourceFactory {

    public HikariDataSourceFactory() {
        this.dataSource = new HikariDataSource();
    }
}
