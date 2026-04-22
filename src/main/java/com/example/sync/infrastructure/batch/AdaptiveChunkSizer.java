package com.example.sync.infrastructure.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class AdaptiveChunkSizer {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveChunkSizer.class);

    private final int min;
    private final int max;
    private final AtomicInteger current;

    public AdaptiveChunkSizer(int initial, int min, int max) {
        this.min = min;
        this.max = max;
        this.current = new AtomicInteger(initial);
    }

    public int getChunkSize() { return current.get(); }

    public void adjust(long insertMs) {
        int cur = current.get();
        int next;

        if (insertMs > 40000) {
            next = Math.max(min, (int)(cur * 0.7));
            log.warn("[Chunk] insert {}ms 초과 → {} → {} (축소)", insertMs, cur, next);
        } else if (insertMs < 10000) {
            next = Math.min(max, (int)(cur * 1.2));
            log.debug("[Chunk] insert {}ms 양호 → {} → {} (확대)", insertMs, cur, next);
        } else {
            return;
        }

        current.set(next);
    }
}
