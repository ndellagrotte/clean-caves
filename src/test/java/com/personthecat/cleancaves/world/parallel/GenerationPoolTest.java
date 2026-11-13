package com.personthecat.cleancaves.world.parallel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The helper pool has to run every task exactly once, on every path, and surface failures. */
class GenerationPoolTest {

    private static final long SPIN_LIMIT_NANOS = TimeUnit.SECONDS.toNanos(2);

    private ThreadPoolExecutor executor;

    @AfterEach
    void stopPool() {
        if (this.executor != null) {
            this.executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("the inline path runs everything on the calling thread")
    void inlineRunsOnCaller() {
        final ParallelSite site = new ParallelSite("test.inline");
        final AtomicInteger count = new AtomicInteger();
        final Set<Thread> threads = ConcurrentHashMap.newKeySet();
        GenerationPool.runInline(site, 100, 100L, i -> {
            threads.add(Thread.currentThread());
            count.incrementAndGet();
        });
        assertThat(count.get()).isEqualTo(100);
        assertThat(threads).containsExactly(Thread.currentThread());
    }

    @Test
    @DisplayName("the parallel path runs every task exactly once across several threads")
    void parallelRunsEachTaskOnce() {
        Assumptions.assumeTrue(Runtime.getRuntime().availableProcessors() > 1);
        final ParallelSite site = new ParallelSite("test.parallel");
        final AtomicLong sum = new AtomicLong();
        final AtomicInteger count = new AtomicInteger();
        final Set<Thread> threads = ConcurrentHashMap.newKeySet();
        GenerationPool.runParallel(site, pool(3), 3, 3000, 3000L, i -> {
            threads.add(Thread.currentThread());
            // Keep the first task busy until a second thread has claimed one, so the test does
            // not depend on the scheduler handing out both tasks quickly enough.
            if (i == 0) {
                final long deadline = System.nanoTime() + SPIN_LIMIT_NANOS;
                while (threads.size() < 2 && System.nanoTime() < deadline) {
                    Thread.onSpinWait();
                }
            }
            sum.addAndGet(i);
            count.incrementAndGet();
        });
        assertThat(count.get()).isEqualTo(3000);
        assertThat(sum.get()).isEqualTo(2999L * 3000L / 2L);
        assertThat(threads.size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("the calling thread keeps working when every helper is busy")
    void callerParticipates() {
        Assumptions.assumeTrue(Runtime.getRuntime().availableProcessors() > 1);
        final ThreadPoolExecutor pool = pool(1);
        final CountDownLatch release = new CountDownLatch(1);
        // Occupy the only helper so that all of the work has to fall to the calling thread.
        pool.execute(() -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        final Set<Thread> threads = ConcurrentHashMap.newKeySet();
        GenerationPool.runParallel(new ParallelSite("test.caller"), pool, 1, 100, 100L, i -> {
            threads.add(Thread.currentThread());
            if (i == 99) {
                release.countDown();
            }
        });
        assertThat(threads).containsExactly(Thread.currentThread());
    }

    @Test
    @DisplayName("an exception in any task reaches the caller")
    void errorsPropagate() {
        final IllegalStateException failure = new IllegalStateException("boom");
        assertThatThrownBy(() -> GenerationPool.runParallel(new ParallelSite("test.error"), pool(2), 2, 100, 100L,
                i -> {
                    if (i == 17) {
                        throw failure;
                    }
                })).isSameAs(failure);
    }

    @Test
    @DisplayName("with no helpers, every task runs on the calling thread")
    void noHelpersRunsInline() {
        final Set<Thread> threads = ConcurrentHashMap.newKeySet();
        final AtomicInteger count = new AtomicInteger();
        GenerationPool.runParallel(new ParallelSite("test.no-helpers"), pool(2), 0, 8, 8L, i -> {
            threads.add(Thread.currentThread());
            count.incrementAndGet();
        });
        assertThat(count.get()).isEqualTo(8);
        assertThat(threads).containsExactly(Thread.currentThread());
    }

    private ThreadPoolExecutor pool(int threads) {
        this.executor = new ThreadPoolExecutor(threads, threads, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                runnable -> {
                    final Thread thread = new Thread(runnable, "test-worker");
                    thread.setDaemon(true);
                    return thread;
                });
        return this.executor;
    }

}
