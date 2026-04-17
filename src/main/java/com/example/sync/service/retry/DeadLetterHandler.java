package com.example.sync.service.retry;

import com.example.sync.service.monitoring.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeadLetterHandler {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterHandler.class);

    private final RetryQueueRepository repo;
    private final AlertService alertService;

    public DeadLetterHandler(RetryQueueRepository repo, AlertService alertService) {
        this.repo = repo;
        this.alertService = alertService;
    }

    public void handle(BatchRetryQueue item) {
        repo.markDead(item.getId());
        String msg = String.format("🚨 [DeadLetter] 재시도 한도 초과: ID=%s, Error=%s", String.valueOf(item.getId()), item.getErrorType());
        log.error(msg);
        alertService.sendAlert(msg);
    }
}
