# Oracle-to-Oracle Data Sync Agent

Oracle 데이터베이스 간 대용량 데이터를 안정적으로 동기화하는 Spring Boot 기반 에이전트입니다.
JPA 없이 순수 JdbcTemplate만 사용하여 의존성을 최소화하고, 새로운 동기화 잡 추가 시 어댑터 클래스 하나만 작성하면 됩니다.

## 아키텍처

```
Source DB ──읽기──▶ SyncJobExecutor ──MERGE INTO──▶ Target DB
                         │
                  CheckpointService ──read/write──▶ Proxy DB
                  RetryQueueRepository                (sync_checkpoint)
                  ShedLock                            (batch_retry_queue)
                                                      (shedlock)
```

### 3-DB 분리
| 역할 | 스키마 | 용도 |
|------|-------|------|
| Source | `source` | 동기화 원천 데이터 읽기 전용 |
| Target | `target` | MERGE INTO로 Upsert |
| Proxy | `proxy` | 체크포인트, 재시도 큐, ShedLock |

## 핵심 동작 흐름

1. **Scheduler (`OrdersSyncScheduler`)** — `@Scheduled` + `@SchedulerLock`으로 단일 인스턴스 실행 보장
2. **Executor (`SyncJobExecutor`)** — `LockAssert.assertLocked()` 호출 후 청크 루프 진입
3. **Adapter (`SyncJobAdapter<T>`)** — 테이블별 구현체. `readChunk` / `bulkUpsert` / `checkLag`
4. **Checkpoint** — `bumpForward` UPDATE(`WHERE last_id < :lastId`)로 단조 증가 보장. split-brain에서 역진 방지.
5. **Retry Queue** — `FOR UPDATE SKIP LOCKED`로 원자적 Claim. 재시도 횟수 초과 시 Dead Letter 처리.

## 새로운 동기화 잡 추가 방법

1. `SyncJobAdapter<T>` 구현체 작성 (`XxxSyncAdapter.java`)
2. `XxxSyncScheduler.java` 작성 (`@Scheduled` + `@SchedulerLock`)
3. `application.yml`에 `sync.xxx.*` 설정 추가

Source 조회, Target Upsert, Lag 체크 로직만 어댑터에 캡슐화되므로 설정·인프라 코드 변경 없음.

## 패키지 구조

```
src/main/java/com/example/sync/
├── service/
│   ├── SyncJobExecutor.java          # 청크 루프, 예외 분류, 메트릭 기록
│   ├── OrdersSyncScheduler.java      # @Scheduled + @SchedulerLock
│   ├── SyncSweeperScheduler.java     # PROCESSING stuck 복구, SUCCESS 정리
│   ├── adapter/
│   │   ├── SyncJobAdapter.java       # 어댑터 인터페이스
│   │   └── OrdersSyncAdapter.java    # ORDERS 테이블 구현체
│   ├── checkpoint/
│   │   └── CheckpointService.java    # last_id 조회/단조증가 업데이트
│   ├── retry/
│   │   ├── RetryQueueProcessor.java  # Claim → 재처리 → 성공/DLQ
│   │   ├── RetryQueueRepository.java # claimPending (REQUIRES_NEW)
│   │   ├── BatchRetryQueue.java      # 재시도 큐 DTO
│   │   ├── RetryStatus.java          # PENDING / PROCESSING / SUCCESS / DEAD
│   │   └── DeadLetterHandler.java
│   └── monitoring/
│       ├── SyncMetrics.java          # Prometheus 메트릭
│       ├── LagMonitor.java           # Lag 계산 및 알림
│       └── AlertService.java
├── repository/
│   ├── SyncCheckpointRepository.java # JdbcTemplate 기반 checkpoint CRUD
│   └── BatchRetryQueueJdbcRepository.java
├── infrastructure/
│   ├── batch/AdaptiveChunkSizer.java # 쓰기 속도에 따른 청크 사이즈 자동 조절
│   ├── exception/
│   │   ├── OracleExceptionClassifier.java  # ORA 코드 → Transient/Permanent 분류
│   │   ├── TransientSyncException.java
│   │   └── PermanentSyncException.java
│   ├── health/SyncDataSourcesHealthIndicator.java
│   ├── config/
│   │   ├── DataSourceConfig.java     # source/target/proxy DataSource + JdbcTemplate
│   │   ├── ProxyJdbcConfig.java      # proxyTransactionManager (DataSourceTransactionManager)
│   │   ├── TargetJdbcConfig.java     # targetTransactionManager
│   │   ├── ShedLockConfig.java       # JdbcTemplateLockProvider
│   │   ├── TaskConfig.java           # ThreadPoolTaskScheduler
│   │   └── RetryConfig.java
│   └── MeasuringLockProvider.java
└── dto/
    └── OrdersRecordDto.java
```

## 설정

### application.yml 주요 항목

```yaml
spring:
  datasource:
    source:
      jdbc-url: jdbc:oracle:thin:@localhost:1521/FREEPDB1
      username: source
      password: 1234
    target:
      jdbc-url: jdbc:oracle:thin:@localhost:1521/FREEPDB1
      username: target
      password: 1234
    proxy:
      jdbc-url: jdbc:oracle:thin:@localhost:1521/FREEPDB1
      username: proxy
      password: 1234

sync:
  orders:
    enabled: true
    fixed-delay-ms: 60000        # 스케줄 간격 (ms)
    chunk-size-initial: 2000     # 초기 청크 사이즈
    chunk-size-min: 500
    chunk-size-max: 10000
    lag-alert-threshold: 100000  # Source-Target 격차 알림 임계값
    retry:
      max-retry: 3
      initial-backoff-sec: 60
      backoff-step-sec: 300
      claim-batch-size: 50

server:
  port: 8090
```

### DB 스키마 (Proxy)

```sql
-- 동기화 체크포인트
CREATE TABLE sync_checkpoint (
    job_name      VARCHAR2(100) PRIMARY KEY,
    last_id       NUMBER(19)    DEFAULT 0 NOT NULL,
    last_synced_at TIMESTAMP,
    processed_cnt NUMBER(19)    DEFAULT 0,
    chunk_size    NUMBER(10)
);

-- 재시도 큐
CREATE TABLE batch_retry_queue (
    id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_ids    CLOB,
    error_type    VARCHAR2(100),
    error_message CLOB,
    retry_count   NUMBER(5)    DEFAULT 0,
    max_retry     NUMBER(5)    DEFAULT 3,
    next_retry_at TIMESTAMP,
    status        VARCHAR2(20) DEFAULT 'PENDING',
    job_name      VARCHAR2(50) DEFAULT 'ORDERS_SYNC' NOT NULL
);
CREATE INDEX idx_retry_job_status_next ON batch_retry_queue (job_name, status, next_retry_at);

-- ShedLock
CREATE TABLE shedlock (
    name       VARCHAR2(64)  PRIMARY KEY,
    lock_until TIMESTAMP(3)  NOT NULL,
    locked_at  TIMESTAMP(3)  NOT NULL,
    locked_by  VARCHAR2(255) NOT NULL
);
```

## 실행

```bash
# 빌드
mvn clean package -DskipTests

# 실행
java -jar target/oracle-sync-agent-0.0.1-SNAPSHOT.jar

# Docker 테스트 (호스트 Oracle 사용)
docker build -t oracle-sync-agent-test .
docker run --rm -e DB_HOST=host.docker.internal --add-host=host.docker.internal:host-gateway oracle-sync-agent-test
```

## 모니터링

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /actuator/health` | liveness / readiness 프로브 (K8s 대응) |
| `GET /actuator/prometheus` | Prometheus 스크레이핑 |

### 주요 메트릭

| 메트릭 | 설명 |
|--------|------|
| `sync.tick.duration` | 전체 tick 소요 시간 |
| `sync.read.duration` | Source 조회 소요 시간 |
| `sync.write.duration` | Target Upsert 소요 시간 |
| `sync.lag.count` | Source-Target 격차 (row 수) |
| `sync.checkpoint.regression_skip` | split-brain 역진 방지 카운트 |
| `sync.retry.enqueue` | 재시도 큐 적재 건수 |
| `sync.deadletter.enqueue` | Dead Letter 처리 건수 |
| `sync.shedlock.acquired` | ShedLock 획득 횟수 |

## 기술 스택

- Java 8, Spring Boot 2.7.18
- **JdbcTemplate** (JPA 없음) — Oracle `MERGE INTO`, `FOR UPDATE SKIP LOCKED`, `SYSTIMESTAMP`
- **ShedLock 4.44** — 분산 스케줄 락
- **HikariCP** — 커넥션 풀 (source/target/proxy 각 독립)
- **ojdbc8 19.22** — Oracle JDBC
- **Micrometer + Prometheus** — 메트릭
