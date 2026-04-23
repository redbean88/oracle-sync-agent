package com.example.sync.infrastructure.config;

import com.example.sync.infrastructure.MeasuringLockProvider;
import com.example.sync.service.monitoring.SyncMetrics;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(@Qualifier("proxyDataSource") DataSource dataSource,
                                     SyncMetrics metrics) {
        LockProvider jdbc = new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new org.springframework.jdbc.core.JdbcTemplate(dataSource))
            .usingDbTime()
            .build()
        );
        return new MeasuringLockProvider(jdbc, metrics);
    }
}
