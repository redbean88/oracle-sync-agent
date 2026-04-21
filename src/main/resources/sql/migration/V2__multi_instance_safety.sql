-- V2: 멀티 인스턴스 배포 안전성 보강 마이그레이션
-- 적용 대상 DB: Proxy

-- P0-2: Checkpoint 낙관적 락 (단조 UPDATE 보조)
ALTER TABLE sync_checkpoint ADD (version NUMBER(19) DEFAULT 0 NOT NULL);

-- P1-9: AdaptiveChunkSizer 영속화 (cold start 완화)
ALTER TABLE sync_checkpoint ADD (chunk_size NUMBER(10));

-- P0-3: Retry Queue 인덱스 (claim 쿼리 및 DLQ 모니터링)
CREATE INDEX idx_retry_pending_next
    ON batch_retry_queue (status, next_retry_at);
CREATE INDEX idx_retry_status
    ON batch_retry_queue (status);

-- P0-3: status 값 일원화 (과거 "DLQ" → "DEAD")
UPDATE batch_retry_queue SET status = 'DEAD' WHERE status = 'DLQ';
COMMIT;
