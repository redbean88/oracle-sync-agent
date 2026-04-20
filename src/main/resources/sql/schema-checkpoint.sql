-- 체크포인트
CREATE TABLE sync_checkpoint (
    job_name       VARCHAR(100) PRIMARY KEY,
    last_id        NUMBER(19)   DEFAULT 0,
    last_synced_at TIMESTAMP,
    processed_cnt  NUMBER(19)   DEFAULT 0,
    version        NUMBER(19)   DEFAULT 0 NOT NULL,
    chunk_size     NUMBER(10)
);
