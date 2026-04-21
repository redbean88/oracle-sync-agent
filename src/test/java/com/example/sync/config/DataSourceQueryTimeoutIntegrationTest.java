package com.example.sync.config;

import com.example.sync.AbstractOracleIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataSourceConfig의 queryTimeout, fetchSize 설정값 검증.
 */
@SpringBootTest
class DataSourceQueryTimeoutIntegrationTest extends AbstractOracleIntegrationTest {

    @Autowired @Qualifier("sourceJdbcTemplate") private JdbcTemplate sourceJdbc;
    @Autowired @Qualifier("targetJdbcTemplate") private JdbcTemplate targetJdbc;
    @Autowired @Qualifier("proxyJdbcTemplate")  private JdbcTemplate proxyJdbc;

    @Test
    void sourceJdbcTemplate_queryTimeout_540초() {
        assertThat(sourceJdbc.getQueryTimeout()).isEqualTo(540);
    }

    @Test
    void targetJdbcTemplate_queryTimeout_540초() {
        assertThat(targetJdbc.getQueryTimeout()).isEqualTo(540);
    }

    @Test
    void proxyJdbcTemplate_queryTimeout_540초() {
        assertThat(proxyJdbc.getQueryTimeout()).isEqualTo(540);
    }

    @Test
    void sourceJdbcTemplate_fetchSize_5000() {
        assertThat(sourceJdbc.getFetchSize()).isEqualTo(5000);
    }
}
