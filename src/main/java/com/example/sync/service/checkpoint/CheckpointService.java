package com.example.sync.service.checkpoint;

import com.example.sync.repository.SyncCheckpointRepository;
import com.example.sync.service.monitoring.SyncMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckpointService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointService.class);

    private final SyncCheckpointRepository repository;
    private final SyncMetrics metrics;

    public CheckpointService(SyncCheckpointRepository repository, SyncMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    @Transactional(transactionManager = "proxyTransactionManager", readOnly = true)
    public long getLastId(String jobName) {
        return repository.findLastId(jobName);
    }

    @Transactional(transactionManager = "proxyTransactionManager")
    public void update(String jobName, long lastId, int processedCnt, Integer chunkSize) {
        int updated = repository.bumpForward(jobName, lastId, processedCnt, chunkSize);
        if (updated == 0) {
            if (!repository.existsById(jobName)) {
                repository.insert(jobName, lastId, processedCnt, chunkSize);
            } else {
                log.warn("[Checkpoint] 역진 방지 - 기존 lastId가 {} 이상이므로 갱신 생략 (jobName={})",
                        lastId, jobName);
                metrics.incrementCheckpointRegression();
            }
        }
    }
}
