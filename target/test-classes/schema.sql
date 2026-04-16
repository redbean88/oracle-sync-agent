-- Source DB 테이블
CREATE TABLE IF NOT EXISTS orders (
    id          BIGINT PRIMARY KEY,
    order_no    VARCHAR(50),
    customer_id BIGINT,
    status      VARCHAR(20),
    amount      DECIMAL(10,2),
    created_at  TIMESTAMP
);

-- Target DB 테이블 (본 테스트에서는 편의상 같은 메모리에 있다고 치거나, 스키마 분리를 생략하고 공용으로 생성합니다. jdbc template으로 각각 조회 가능)
CREATE TABLE IF NOT EXISTS orders_target (
    id          BIGINT PRIMARY KEY,
    order_no    VARCHAR(50),
    customer_id BIGINT,
    status      VARCHAR(20),
    amount      DECIMAL(10,2),
    created_at  TIMESTAMP
);
