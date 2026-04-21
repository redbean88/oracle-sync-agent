package com.example.sync.infrastructure.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncDataSourcesHealthIndicatorTest {

    @Mock private DataSource sourceDs;
    @Mock private DataSource targetDs;
    @Mock private DataSource proxyDs;
    @Mock private Connection connection;
    @Mock private Statement statement;

    private SyncDataSourcesHealthIndicator indicator;

    @BeforeEach
    void setUp() throws Exception {
        indicator = new SyncDataSourcesHealthIndicator(sourceDs, targetDs, proxyDs);
        // 기본적으로 모든 DataSource는 정상 응답
        when(sourceDs.getConnection()).thenReturn(connection);
        when(targetDs.getConnection()).thenReturn(connection);
        when(proxyDs.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
    }

    @Test
    void health_3개_모두_UP() {
        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.UP);
        assertThat(result.getDetails()).containsKeys("source", "target", "proxy");
        assertThat(result.getDetails().get("source")).isEqualTo("UP");
        assertThat(result.getDetails().get("target")).isEqualTo("UP");
        assertThat(result.getDetails().get("proxy")).isEqualTo("UP");
    }

    @Test
    void health_source_DOWN() throws Exception {
        when(sourceDs.getConnection()).thenThrow(new SQLException("source 연결 실패"));

        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.DOWN);
        assertThat(result.getDetails().get("source").toString()).contains("DOWN");
        assertThat(result.getDetails().get("target")).isEqualTo("UP");
    }

    @Test
    void health_target_DOWN() throws Exception {
        when(targetDs.getConnection()).thenThrow(new SQLException("target 연결 실패"));

        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.DOWN);
        assertThat(result.getDetails().get("target").toString()).contains("DOWN");
    }

    @Test
    void health_proxy_DOWN() throws Exception {
        when(proxyDs.getConnection()).thenThrow(new SQLException("proxy 연결 실패"));

        Health result = indicator.health();

        assertThat(result.getStatus()).isEqualTo(Status.DOWN);
        assertThat(result.getDetails().get("proxy").toString()).contains("DOWN");
    }

    @Test
    void health_detail에_DOWN_메시지_포함() throws Exception {
        String errorMsg = "Connection refused";
        when(targetDs.getConnection()).thenThrow(new SQLException(errorMsg));

        Health result = indicator.health();

        assertThat(result.getDetails().get("target").toString()).contains("DOWN");
    }
}
