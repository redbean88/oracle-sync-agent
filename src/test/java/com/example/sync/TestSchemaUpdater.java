package com.example.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 테스트 프로파일에서만 실행. 호스트 Oracle의 구 스키마와 신규 코드 간 불일치를 자동 보정한다.
 * - batch_retry_queue.job_name: 멀티 잡 지원을 위해 추가된 컬럼
 * - orders id trigger: id 컬럼에 DEFAULT/IDENTITY가 없는 환경에서 자동 채번
 */
@Component
@Profile("test")
public class TestSchemaUpdater {

    private static final Logger log = LoggerFactory.getLogger(TestSchemaUpdater.class);

    @Autowired @Qualifier("proxyJdbcTemplate")  private JdbcTemplate proxyJdbc;
    @Autowired @Qualifier("sourceJdbcTemplate") private JdbcTemplate sourceJdbc;

    @PostConstruct
    public void migrate() {
        addJobNameColumn();
        addJobNameIndex();
        ensureOrdersAutoId();
    }

    private void addJobNameColumn() {
        try {
            proxyJdbc.execute(
                "ALTER TABLE batch_retry_queue ADD (job_name VARCHAR2(50) DEFAULT 'ORDERS_SYNC' NOT NULL)");
            log.info("[TestSchema] batch_retry_queue.job_name 컬럼 추가 완료");
        } catch (Exception e) {
            // ORA-01430: column already exists — 정상
            log.debug("[TestSchema] batch_retry_queue.job_name 이미 존재: {}", e.getMessage());
        }
    }

    private void addJobNameIndex() {
        try {
            proxyJdbc.execute(
                "CREATE INDEX idx_retry_job_status_next ON batch_retry_queue (job_name, status, next_retry_at)");
        } catch (Exception e) {
            // ORA-00955: name already used — 정상
        }
    }

    private void ensureOrdersAutoId() {
        // identity 컬럼이면 Oracle이 자동 채번하므로 트리거 불필요.
        // GENERATED ALWAYS AS IDENTITY에 트리거로 :NEW.id를 쓰면 ORA-04063(trigger has errors)이 발생하므로
        // user_tab_identity_cols로 먼저 확인한다.
        try {
            Integer hasIdentity = sourceJdbc.queryForObject(
                "SELECT COUNT(*) FROM user_tab_identity_cols WHERE table_name = 'ORDERS' AND column_name = 'ID'",
                Integer.class);
            if (hasIdentity != null && hasIdentity > 0) {
                log.debug("[TestSchema] orders.id는 identity 컬럼이므로 트리거 생략");
                return;
            }
        } catch (Exception e) {
            log.debug("[TestSchema] identity 컬럼 조회 오류: {}", e.getMessage());
            return;
        }

        // id에 DEFAULT/IDENTITY가 없는 환경에서만 실행
        try {
            sourceJdbc.execute(
                "CREATE SEQUENCE orders_id_seq START WITH 10000 INCREMENT BY 1 NOCACHE");
            log.info("[TestSchema] orders_id_seq 시퀀스 생성 완료");
        } catch (Exception e) {
            // ORA-00955: already exists — 정상
        }
        try {
//            sourceJdbc.execute(
//                "CREATE OR REPLACE TRIGGER trg_orders_id_default " +
//                "BEFORE INSERT ON orders FOR EACH ROW " +
//                "WHEN (NEW.id IS NULL) " +
//                "BEGIN " +
//                "  :NEW.id := orders_id_seq.NEXTVAL; " +
//                "END");
//            log.info("[TestSchema] trg_orders_id_default 트리거 생성 완료");
        } catch (Exception e) {
            log.debug("[TestSchema] trg_orders_id_default 오류: {}", e.getMessage());
        }
    }
}
