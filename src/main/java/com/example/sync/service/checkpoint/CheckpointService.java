package com.example.sync.service.checkpoint;

import com.example.sync.domain.proxy.SyncCheckpoint;
import com.example.sync.repository.proxy.SyncCheckpointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CheckpointService {

    private final SyncCheckpointRepository repository;

    public CheckpointService(SyncCheckpointRepository repository) {
        this.repository = repository;
    }

    @Transactional(transactionManager = "proxyTransactionManager", readOnly = true)
    public long getLastId(String jobName) {
        return repository.findById(jobName)
                .map(SyncCheckpoint::getLastId)
                .orElse(0L);
    }

    @Transactional(transactionManager = "proxyTransactionManager")
    public void update(String jobName, long lastId, int processedCnt) {
        SyncCheckpoint checkpoint = repository.findById(jobName).orElse(null);
        if (checkpoint != null) {
            checkpoint.setLastId(lastId);
            checkpoint.setLastSyncedAt(LocalDateTime.now());
            checkpoint.setProcessedCnt(checkpoint.getProcessedCnt() + processedCnt);
        } else {
            checkpoint = SyncCheckpoint.builder()
                .jobName(jobName)
                .lastId(lastId)
                .lastSyncedAt(LocalDateTime.now())
                .processedCnt((long) processedCnt)
                .build();
        }
        repository.save(checkpoint);
    }
}
