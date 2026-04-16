package com.example.sync.retry;

import com.example.sync.reader.dto.SourceRecordDto;
import com.example.sync.writer.TargetDataWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryQueueProcessor {

    private final RetryQueueRepository repo;
    private final TargetDataWriter writer;
    private final DeadLetterHandler dlqHandler;

    public void enqueue(List<SourceRecordDto> records, Exception e) {
        String ids = records.stream()
            .map(r -> String.valueOf(r.getId()))
            .collect(Collectors.joining(","));

        BatchRetryQueue entry = BatchRetryQueue.builder()
            .sourceIds(ids)
            .errorType(classifyError(e))
            .errorMessage(e.getMessage())
            .retryCount(0)
            .maxRetry(5)
            .nextRetryAt(LocalDateTime.now().plusMinutes(5))
            .status("PENDING")
            .build();

        repo.save(entry);
        log.warn("[RetryQueue] {} 건 PENDING 등록", records.size());
    }

    public void processQueue() {
        List<BatchRetryQueue> pending = repo.findRetryable(LocalDateTime.now());
        for (BatchRetryQueue item : pending) {
            if (item.getRetryCount() >= item.getMaxRetry()) {
                dlqHandler.handle(item);
                continue;
            }
            try {
                // 실 서비스에서는 sourceIds를 분할하여 SourceDB에서 재조회 후 bulkUpsert 호출
                // 여기서는 생략된 형태만 유지
                // List<SourceRecordDto> params = reader.fetchByIds(item.getSourceIds());
                // writer.bulkUpsert(params);
                
                repo.markSuccess(item.getId());
            } catch (Exception e) {
                repo.incrementRetry(item.getId(),
                    LocalDateTime.now().plusMinutes(exponentialBackoff(item.getRetryCount())));
            }
        }
    }

    private String classifyError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("ORA-02291") || msg.contains("ORA-01722")) return "PERMANENT";
        return "TRANSIENT";
    }

    private int exponentialBackoff(int retryCount) {
        return (int) Math.pow(2, retryCount);  // 1 / 2 / 4 / 8 / 16분
    }
}
