package com.example.sync;

import org.springframework.test.context.ActiveProfiles;

/**
 * 로컬 Oracle DB(application-test.yml의 jdbc-url) 기반 통합 테스트 공통 베이스 클래스.
 * DB는 호스트 Oracle(FREEPDB1)을 직접 사용한다.
 */
@ActiveProfiles("test")
public abstract class AbstractOracleIntegrationTest {
}
