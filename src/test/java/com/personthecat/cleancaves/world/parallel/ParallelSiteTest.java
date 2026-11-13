package com.personthecat.cleancaves.world.parallel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The workload detector decides whether a split is worth its coordination cost. */
class ParallelSiteTest {

    @Test
    @DisplayName("a site that has never been timed stays inline")
    void untimedStaysInline() {
        final ParallelSite site = new ParallelSite("test");
        assertThat(site.nsPerUnit()).isNegative();
        assertThat(site.shouldParallelize(1_000_000L)).isFalse();
    }

    @Test
    @DisplayName("forcing a site overrides the measurements")
    void forcedModes() {
        final ParallelSite site = new ParallelSite("test");
        site.force(1);
        assertThat(site.shouldParallelize(1L)).isTrue();
        site.force(-1);
        assertThat(site.shouldParallelize(1_000_000_000L)).isFalse();
        site.force(0);
        assertThat(site.shouldParallelize(1_000_000L)).isFalse();
    }

    @Test
    @DisplayName("work below the hand-off cost is never split")
    void smallWorkloadsStayInline() {
        final ParallelSite site = new ParallelSite("test");
        site.recordInline(1000L, 1_000_000L); // 1 microsecond per unit
        assertThat(site.nsPerUnit()).isCloseTo(1000.0D, org.assertj.core.data.Offset.offset(0.0001D));
        // Ten units are 10 microseconds: below the 50 microsecond threshold.
        assertThat(site.shouldParallelize(10L)).isFalse();
        // A thousand units are a millisecond: large enough to be worth splitting.
        assertThat(site.shouldParallelize(1000L)).isTrue();
    }

    @Test
    @DisplayName("a size class that keeps losing time is not split again")
    void slowSplitsAreLearned() {
        final ParallelSite site = new ParallelSite("test");
        site.recordInline(1000L, 1_000_000L);
        for (int i = 0; i < 4; i++) {
            site.recordParallel(1000L, 2_000_000L, 1_000_000L); // twice as slow as inline
        }
        assertThat(site.speedup(1000L)).isLessThan(1.0D);
        assertThat(site.shouldParallelize(1000L)).isFalse();
    }

    @Test
    @DisplayName("a size class that wins time keeps being split")
    void fastSplitsAreKept() {
        final ParallelSite site = new ParallelSite("test");
        site.recordInline(1000L, 1_000_000L);
        for (int i = 0; i < 4; i++) {
            site.recordParallel(1000L, 200_000L, 1_000_000L); // five times faster
        }
        assertThat(site.speedup(1000L)).isGreaterThan(4.0D);
        assertThat(site.shouldParallelize(1000L)).isTrue();
    }

}
