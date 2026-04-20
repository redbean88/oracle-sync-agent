-- 재시도 큐
CREATE TABLE batch_retry_queue (
    id            NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_ids    CLOB,
    error_type    VARCHAR(100),
    error_message CLOB,
    retry_count   NUMBER(10)  DEFAULT 0,
    max_retry     NUMBER(10)  DEFAULT 5,
    next_retry_at TIMESTAMP,
    status        VARCHAR(20) DEFAULT 'PENDING'
);

CREATE INDEX idx_retry_pending_next ON batch_retry_queue (status, next_retry_at);
CREATE INDEX idx_retry_status       ON batch_retry_queue (status);
