package com.example.sync.infra.retry;

import com.example.sync.domain.dto.SourceRecordDto;
import com.example.sync.repository.writer.TargetDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetryQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(RetryQueueProcessor.class);

    private final RetryQueueRepository repo;
    private final TargetDataWriter writer;
    private final DeadLetterHandler dlqHandler;

    public RetryQueueProcessor(RetryQueueRepository repo, TargetDataWriter writer, DeadLetterHandler dlqHandler) {
        this.repo = repo;
        this.writer = writer;
        this.dlqHandler = dlqHandler;
    }

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
        return (int) Math.pow(2, retryCount);
    }
}
