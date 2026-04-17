package com.example.sync.domain.proxy;

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

    private SyncCheckpoint(Builder builder) {
        this.jobName = builder.jobName;
        this.lastId = builder.lastId;
        this.lastSyncedAt = builder.lastSyncedAt;
        this.processedCnt = builder.processedCnt;
    }

    public static Builder builder() {
        return new Builder();
    }


    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    
    public Long getLastId() { return lastId; }
    public void setLastId(Long lastId) { this.lastId = lastId; }
    
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    
    public Long getProcessedCnt() { return processedCnt; }
    public void setProcessedCnt(Long processedCnt) { this.processedCnt = processedCnt; }

    public static class Builder {
        private String jobName;
        private Long lastId = 0L;
        private LocalDateTime lastSyncedAt;
        private Long processedCnt = 0L;

        public Builder jobName(String jobName) {
            this.jobName = jobName;
            return this;
        }

        public Builder lastId(Long lastId) {
            this.lastId = lastId;
            return this;
        }

        public Builder lastSyncedAt(LocalDateTime lastSyncedAt) {
            this.lastSyncedAt = lastSyncedAt;
            return this;
        }

        public Builder processedCnt(Long processedCnt) {
            this.processedCnt = processedCnt;
            return this;
        }

        public SyncCheckpoint build() {
            return new SyncCheckpoint(this);
        }
    }
}
