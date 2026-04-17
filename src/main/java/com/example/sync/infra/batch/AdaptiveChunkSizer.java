package com.example.sync.infra.batch;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AdaptiveChunkSizer {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveChunkSizer.class);

    @Value("${sync.chunk-size-initial:2000}") private int initial;
    @Value("${sync.chunk-size-min:500}")      private int min;
    @Value("${sync.chunk-size-max:10000}")    private int max;

    private final AtomicInteger current = new AtomicInteger();

    @PostConstruct
    void init() { current.set(initial); }

    public int currentSize() { return current.get(); }

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
