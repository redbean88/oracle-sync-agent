package com.example.sync.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.beans.factory.annotation.Qualifier;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.source")
    public DataSource sourceDataSource() {
        return new HikariDataSource(); // Explicitly use Hikari
    }

    @Bean("targetDataSource")
    @ConfigurationProperties("spring.datasource.target")
    public DataSource targetDataSource() {
        return new HikariDataSource(); // Explicitly use Hikari
    }

    @Bean
    @Primary
    public JdbcTemplate sourceJdbcTemplate(@Qualifier("sourceDataSource") DataSource ds) {
        JdbcTemplate jt = new JdbcTemplate(ds);
        jt.setFetchSize(1000);
        return jt;
    }

    @Bean("targetJdbcTemplate")
    public JdbcTemplate targetJdbcTemplate(@Qualifier("targetDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

}
