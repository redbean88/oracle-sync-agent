package com.example.sync.retry;

import com.example.sync.monitoring.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterHandler {

    private final RetryQueueRepository repo;
    private final AlertService alertService;

    public void handle(BatchRetryQueue item) {
        repo.markDead(item.getId());
        String msg = String.format("🚨 [DeadLetter] 재시도 한도 초과: ID=%d, Error=%s", item.getId(), item.getErrorType());
        log.error(msg);
        alertService.sendSlack(msg);
    }
}
