package com.example.sync.infrastructure.retry;

import com.example.sync.dto.SourceRecordDto;
import com.example.sync.repository.reader.SourceDataReader;
import com.example.sync.repository.writer.TargetDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetryQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(RetryQueueProcessor.class);

    private final RetryQueueRepository repo;
    private final SourceDataReader sourceDataReader;
    private final TargetDataWriter targetDataWriter;

    public RetryQueueProcessor(RetryQueueRepository repo, SourceDataReader sourceDataReader, TargetDataWriter targetDataWriter) {
        this.repo = repo;
        this.sourceDataReader = sourceDataReader;
        this.targetDataWriter = targetDataWriter;
    }

    /** 실패 시 ID 리스트만 Proxy DB에 기록 */
    public void enqueue(List<SourceRecordDto> records, Exception e) {
        String ids = records.stream()
                .map(r -> String.valueOf(r.getId()))
                .collect(Collectors.joining(","));

        BatchRetryQueue entry = BatchRetryQueue.builder()
                .sourceIds(ids)
                .errorType(e.getClass().getSimpleName())
                .errorMessage(limitMessage(e.getMessage()))
                .retryCount(0)
                .maxRetry(3)
                .nextRetryAt(LocalDateTime.now().plusMinutes(1)) // 1분 후부터 시도
                .status("PENDING")
                .build();

        repo.save(entry);
        log.warn("[RetryQueue] {} 건의 데이터 ID를 재시도 큐(Proxy DB)에 등록했습니다.", records.size());
    }

    /** 스케줄에 의해 호출되어 실제로 복구(Retry) 수행 */
    @Transactional(transactionManager = "proxyTransactionManager")
    public void processQueue() {
        List<BatchRetryQueue> retryTargets = repo.findRetryable(LocalDateTime.now());
        if (retryTargets.isEmpty()) return;

        for (BatchRetryQueue item : retryTargets) {
            try {
                // 1. 저장된 ID 파싱
                List<Long> ids = Arrays.stream(item.getSourceIds().split(","))
                        .map(Long::valueOf)
                        .collect(Collectors.toList());

                // 2. Source DB에서 데이터 다시 읽기
                List<SourceRecordDto> records = sourceDataReader.fetchByIds(ids);

                if (records.isEmpty()) {
                    log.error("[RetryTask] 원본 데이터를 조회할 수 없습니다. (IDs: {})", item.getSourceIds());
                    item.setStatus("FAILED");
                } else {
                    // 3. 실제 복구 실행
                    targetDataWriter.bulkUpsert(records);
                    item.setStatus("SUCCESS");
                    log.info("[RetryTask] 성공적으로 복구 적합 완료 (IDs: {})", item.getSourceIds());
                }
            } catch (Exception e) {
                // 4. 실패 시 관리
                int currentRetry = item.getRetryCount() + 1;
                if (currentRetry >= item.getMaxRetry()) {
                    item.setStatus("DLQ");
                    log.error("[RetryTask] 최종 재시도 실패 - DLQ 이동 (IDs: {})", item.getSourceIds());
                } else {
                    item.setRetryCount(currentRetry);
                    item.setNextRetryAt(LocalDateTime.now().plusMinutes(5L * currentRetry));
                    log.warn("[RetryTask] 재시도 실패 ({}회), 다음 실행: {}", currentRetry, item.getNextRetryAt());
                }
            }
            repo.save(item);
        }
    }

    private String limitMessage(String msg) {
        if (msg == null) return "";
        return msg.length() > 2000 ? msg.substring(0, 2000) : msg;
    }
}
