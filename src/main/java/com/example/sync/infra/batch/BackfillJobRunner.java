package com.example.sync.infra.batch;

import com.example.sync.repository.reader.SourceDataReader;
import com.example.sync.domain.dto.SourceRecordDto;
import com.example.sync.repository.writer.TargetDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class BackfillJobRunner {

    private static final Logger log = LoggerFactory.getLogger(BackfillJobRunner.class);

    private final SourceDataReader reader;
    private final TargetDataWriter writer;

    public BackfillJobRunner(SourceDataReader reader, TargetDataWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

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
