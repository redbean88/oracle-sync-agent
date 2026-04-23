package com.example.sync.service.adapter;

import java.time.Duration;
import java.util.List;

public interface SyncJobAdapter<T> {

    String getJobName();

    List<T> readChunk(long lastId, int chunkSize);

    List<T> fetchByIds(List<Long> ids);

    void bulkUpsert(List<T> records);

    /** checkpoint 갱신 및 retry queue 저장에 사용할 레코드 ID 추출 */
    long extractLastId(T record);

    /** 어댑터가 직접 source DB lag 계산 후 lagMonitor에 위임 */
    void checkLag(String jobName, long checkpointId, long threshold);

    default Duration getLockAtMostFor() { return Duration.ofMinutes(10); }
    default Duration getLockAtLeastFor() { return Duration.ofSeconds(10); }
}
