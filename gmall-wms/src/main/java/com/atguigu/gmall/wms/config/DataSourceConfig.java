package com.atguigu.gmall.wms.config;


import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;


@Configuration
public class DataSourceConfig {


    @Primary
    @Bean
    @ConfigurationProperties(prefix ="spring.datasource")
    public DataSource dataSource(@Value("${spring.datasource.url}")String jdbcUrl){
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(jdbcUrl);
        return hikariDataSource;
    }
}
