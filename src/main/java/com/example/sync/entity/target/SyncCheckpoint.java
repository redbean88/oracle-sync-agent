package com.example.sync.entity.target;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_checkpoint")
public class SyncCheckpoint {

    @Id
    @Column(name = "job_name", length = 100)
    private String jobName;

    @Column(name = "last_id")
    private Long lastId = 0L;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "processed_cnt")
    private Long processedCnt = 0L;

    public SyncCheckpoint() {}

    public SyncCheckpoint(String jobName, Long lastId, LocalDateTime lastSyncedAt, Long processedCnt) {
        this.jobName = jobName;
        this.lastId = lastId;
        this.lastSyncedAt = lastSyncedAt;
        this.processedCnt = processedCnt;
    }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    
    public Long getLastId() { return lastId; }
    public void setLastId(Long lastId) { this.lastId = lastId; }
    
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    
    public Long getProcessedCnt() { return processedCnt; }
    public void setProcessedCnt(Long processedCnt) { this.processedCnt = processedCnt; }
}
