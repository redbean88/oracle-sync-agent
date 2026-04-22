package com.example.sync.batch;

import com.example.sync.infrastructure.batch.AdaptiveChunkSizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveChunkSizerTest {

    @Test
    void adjust_확대_테스트() {
        AdaptiveChunkSizer sizer = new AdaptiveChunkSizer(2000, 500, 10000);
        int init = sizer.getChunkSize();

        sizer.adjust(9999);

        assertThat(sizer.getChunkSize()).isGreaterThan(init);
    }

    @Test
    void adjust_유지_테스트() {
        AdaptiveChunkSizer sizer = new AdaptiveChunkSizer(2000, 500, 10000);
        int init = sizer.getChunkSize();

        sizer.adjust(10000);

        assertThat(sizer.getChunkSize()).isEqualTo(init);
    }

    @Test
    void adjust_축소_테스트() {
        AdaptiveChunkSizer sizer = new AdaptiveChunkSizer(2000, 500, 10000);
        int init = sizer.getChunkSize();

        sizer.adjust(40001);

        assertThat(sizer.getChunkSize()).isLessThan(init);
    }
}
