package com.example.sync.batch;

import com.example.sync.reader.SourceDataReader;
import com.example.sync.reader.dto.SourceRecordDto;
import com.example.sync.writer.TargetDataWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackfillJobRunner {

    private final SourceDataReader reader;
    private final TargetDataWriter writer;

    // 직접 호출 또는 @Scheduled(cron = "0 0 2 * * *") 새벽 배치
    public void run(long fromId, long toId, int parallelism) {
        long rangeSize = (toId - fromId) / parallelism;
        ExecutorService pool = Executors.newFixedThreadPool(parallelism);

        for (int i = 0; i < parallelism; i++) {
            long start = fromId + rangeSize * i;
            long end   = (i == parallelism - 1) ? toId : start + rangeSize;
            pool.submit(() -> processRange(start, end));
        }
        pool.shutdown();
    }

    private void processRange(long fromId, long toId) {
        long cursor = fromId;
        while (cursor < toId) {
            List<SourceRecordDto> chunk = reader.fetchChunk(cursor, 5000);
            if (chunk.isEmpty()) break;
            writer.bulkUpsert(chunk);
            cursor = chunk.get(chunk.size() - 1).getId();
        }
        log.info("[Backfill] 완료: fromId={} toId={}", fromId, toId);
    }
}
