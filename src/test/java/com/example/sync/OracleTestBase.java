package com.example.sync;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class OracleTestBase {

    @Container
    protected static final OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:slim-faststart")
            .withDatabaseName("testdb")
            .withUsername("sync_user")
            .withPassword("sync_pass");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.source.jdbc-url", oracle::getJdbcUrl);
        registry.add("spring.datasource.source.username", oracle::getUsername);
        registry.add("spring.datasource.source.password", oracle::getPassword);
        
        registry.add("spring.datasource.target.jdbc-url", oracle::getJdbcUrl);
        registry.add("spring.datasource.target.username", oracle::getUsername);
        registry.add("spring.datasource.target.password", oracle::getPassword);
    }
}
