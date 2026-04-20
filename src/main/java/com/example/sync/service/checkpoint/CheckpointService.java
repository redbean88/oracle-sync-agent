package com.example.sync.service.checkpoint;

import com.example.sync.domain.proxy.SyncCheckpoint;
import com.example.sync.repository.proxy.SyncCheckpointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CheckpointService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointService.class);

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

    @Transactional(transactionManager = "proxyTransactionManager", readOnly = true)
    public Integer getChunkSize(String jobName) {
        return repository.findById(jobName)
                .map(SyncCheckpoint::getChunkSize)
                .orElse(null);
    }

    /**
     * 원자적 단조 증가 업데이트. split-brain 상황에서도 checkpoint 역진 방지.
     */
    @Transactional(transactionManager = "proxyTransactionManager")
    public void update(String jobName, long lastId, int processedCnt, Integer chunkSize) {
        int updated = repository.bumpForward(jobName, lastId, LocalDateTime.now(), processedCnt, chunkSize);
        if (updated == 0) {
            if (!repository.existsById(jobName)) {
                SyncCheckpoint fresh = SyncCheckpoint.builder()
                        .jobName(jobName)
                        .lastId(lastId)
                        .lastSyncedAt(LocalDateTime.now())
                        .processedCnt((long) processedCnt)
                        .chunkSize(chunkSize)
                        .build();
                repository.save(fresh);
            } else {
                log.warn("[Checkpoint] 역진 방지 - 기존 lastId가 {} 이상이므로 갱신 생략 (jobName={})",
                        lastId, jobName);
            }
        }
    }
}
