package com.example.sync.service.writer;

import com.example.sync.domain.source.dto.SourceRecordDto;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TargetDataWriter {

    private static final Logger log = LoggerFactory.getLogger(TargetDataWriter.class);

    private final SQLQueryFactory sqlQueryFactory;

    public TargetDataWriter(@Qualifier("targetSqlQueryFactory") SQLQueryFactory sqlQueryFactory) {
        this.sqlQueryFactory = sqlQueryFactory;
    }

    @Transactional(transactionManager = "targetTransactionManager")
    public void bulkUpsert(List<SourceRecordDto> records) {
        if (records.isEmpty()) return;

        // SQLQueryFactory는 RelationalPath 인터페이스를 요구하므로 RelationalPathBase를 정의합니다.
        RelationalPathBase<Object> target = new RelationalPathBase<>(Object.class, "t", null, "orders_target");
        PathBuilder<Object> targetPath = new PathBuilder<>(Object.class, "t");

        // 1. 현재 배치 데이터 중 타겟에 이미 존재하는 ID 목록 조회
        List<Long> ids = records.stream().map(SourceRecordDto::getId).collect(Collectors.toList());
        Set<Long> existingIds = sqlQueryFactory.select(targetPath.get("id", Long.class))
                .from(target)
                .where(targetPath.get("id", Long.class).in(ids))
                .fetch().stream().collect(Collectors.toSet());

        // 2. 배치 처리를 위한 Clause 생성
        SQLUpdateClause updateBatch = sqlQueryFactory.update(target);
        SQLInsertClause insertBatch = sqlQueryFactory.insert(target);

        for (SourceRecordDto record : records) {
            if (existingIds.contains(record.getId())) {
                updateBatch.set(targetPath.get("status", String.class), record.getStatus())
                        .set(targetPath.get("amount", BigDecimal.class), record.getAmount())
                        .set(targetPath.get("order_no", String.class), record.getOrderNo())
                        .where(targetPath.get("id", Long.class).eq(record.getId()))
                        .addBatch();
            } else {
                insertBatch.set(targetPath.get("id", Long.class), record.getId())
                        .set(targetPath.get("order_no", String.class), record.getOrderNo())
                        .set(targetPath.get("customer_id", Long.class), record.getCustomerId())
                        .set(targetPath.get("status", String.class), record.getStatus())
                        .set(targetPath.get("amount", BigDecimal.class), record.getAmount())
                        .set(targetPath.get("created_at", Timestamp.class), Timestamp.valueOf(record.getCreatedAt()))
                        .addBatch();
            }
        }

        long updated = updateBatch.isEmpty() ? 0 : updateBatch.execute();
        long inserted = insertBatch.isEmpty() ? 0 : insertBatch.execute();

        log.info("[Clean-Bulk] Sync completed: {} updated, {} inserted", updated, inserted);
    }
}
