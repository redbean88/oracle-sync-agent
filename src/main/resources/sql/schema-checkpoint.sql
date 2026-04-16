-- 체크포인트
CREATE TABLE sync_checkpoint (
    job_name       VARCHAR(100) PRIMARY KEY,
    last_id        INT          DEFAULT 0,
    last_synced_at TIMESTAMP,
    processed_cnt  INT          DEFAULT 0
);
