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
        return new HikariDataSource();
    }

    @Bean("targetDataSource")
    @ConfigurationProperties("spring.datasource.target")
    public DataSource targetDataSource() {
        return new HikariDataSource();
    }

    @Bean("proxyDataSource")
    @ConfigurationProperties("spring.datasource.proxy")
    public DataSource proxyDataSource() {
        return new HikariDataSource();
    }

    // lockAtMostFor(10분)의 90% — DB 응답 지연 시 tick이 lockAtMostFor를 초과하지 않도록 강제
    private static final int QUERY_TIMEOUT_SEC = 540;

    @Bean
    @Primary
    public JdbcTemplate sourceJdbcTemplate(@Qualifier("sourceDataSource") DataSource ds) {
        JdbcTemplate jt = new JdbcTemplate(ds);
        // chunk-size-max(10000)의 절반 — JDBC row prefetch buffer 메모리 사용과 네트워크 왕복의 절충
        jt.setFetchSize(5000);
        jt.setQueryTimeout(QUERY_TIMEOUT_SEC);
        return jt;
    }

    @Bean("targetJdbcTemplate")
    public JdbcTemplate targetJdbcTemplate(@Qualifier("targetDataSource") DataSource ds) {
        JdbcTemplate jt = new JdbcTemplate(ds);
        jt.setQueryTimeout(QUERY_TIMEOUT_SEC);
        return jt;
    }

    @Bean("proxyJdbcTemplate")
    public JdbcTemplate proxyJdbcTemplate(@Qualifier("proxyDataSource") DataSource ds) {
        JdbcTemplate jt = new JdbcTemplate(ds);
        jt.setQueryTimeout(QUERY_TIMEOUT_SEC);
        return jt;
    }
}
