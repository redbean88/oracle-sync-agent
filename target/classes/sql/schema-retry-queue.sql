-- 재시도 큐
CREATE TABLE batch_retry_queue (
    id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_ids    CLOB,
    error_type    VARCHAR(100),
    error_message CLOB,
    retry_count   INT         DEFAULT 0,
    max_retry     INT         DEFAULT 5,
    next_retry_at TIMESTAMP,
    status        VARCHAR(20) DEFAULT 'PENDING'
);
