package com.example.sync.batch;

import com.example.sync.infrastructure.batch.AdaptiveChunkSizer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveChunkSizerTest {

    @Test
    void adjust_확대_테스트() {
        long insertTime = 9999;
        AdaptiveChunkSizer sizer = new AdaptiveChunkSizer();
        sizer.init();
        int init = sizer.getChunkSize();
        System.out.println("init = " + init);
        assertThat(sizer).isNotNull();

        sizer.adjust(insertTime);

        assertThat(sizer.getChunkSize()).isGreaterThan(init);
    }

    @Test
    void adjust_유지_테스트() {
        long insertTime = 10000;
        AdaptiveChunkSizer sizer = new AdaptiveChunkSizer();
        sizer.init();
        int init = sizer.getChunkSize();
        System.out.println("init = " + init);
        assertThat(sizer).isNotNull();

        sizer.adjust(insertTime);

        assertThat(sizer.getChunkSize()).isEqualTo(init);
    }

    @Test
    void adjust_축소_테스트() {
        long insertTime = 40001;
        AdaptiveChunkSizer sizer = new AdaptiveChunkSizer();
        sizer.init();
        int init = sizer.getChunkSize();
        assertThat(sizer).isNotNull();

        sizer.adjust(insertTime);

        assertThat(sizer.getChunkSize()).isLessThan(init);
    }
}
