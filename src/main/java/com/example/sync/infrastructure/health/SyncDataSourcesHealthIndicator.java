package com.example.sync.infrastructure.health;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * source/target/proxy 세 DataSource 모두 검사.
 * readiness 그룹에 포함되어 K8s readinessProbe에 반영됨.
 */
@Component("syncDatabases")
public class SyncDataSourcesHealthIndicator implements HealthIndicator {

    private final DataSource source;
    private final DataSource target;
    private final DataSource proxy;

    public SyncDataSourcesHealthIndicator(
            @Qualifier("sourceDataSource") DataSource source,
            @Qualifier("targetDataSource") DataSource target,
            @Qualifier("proxyDataSource")  DataSource proxy) {
        this.source = source;
        this.target = target;
        this.proxy  = proxy;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean ok = check("source", source, details)
                   & check("target", target, details)
                   & check("proxy",  proxy,  details);
        return (ok ? Health.up() : Health.down()).withDetails(details).build();
    }

    private boolean check(String name, DataSource ds, Map<String, Object> details) {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.setQueryTimeout(2);
            s.execute("SELECT 1 FROM dual");
            details.put(name, "UP");
            return true;
        } catch (Exception e) {
            details.put(name, "DOWN: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }
}
