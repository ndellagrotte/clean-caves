package com.personthecat.cleancaves.world.parallel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * One hot spot that may be split across helper threads. Each site measures how long a unit of its
 * work takes on the calling thread and how much splitting actually helps, so that a workload is
 * only split when the measurement says it pays for the coordination. The numbers are deliberately
 * cheap and approximate: they only decide whether to split, never what the result is.
 *
 * <p>The speedup of a split is measured from the time helpers actually spent inside the work
 * against its wall time, so queued or idle helpers do not make a split look better than it was.
 *
 * <p>This follows the workload detection used by CaveSpeedThing, which solves the same problem for
 * Better Caves. See the README for the reference.
 */
public final class ParallelSite {

    /** Below this much work, the hand-off costs more than the work itself. */
    private static final long MIN_PARALLEL_NS = 50_000L;

    /** Measurements taken before the speedup of a size class is trusted. */
    private static final int WARMUP_SAMPLES = 4;

    /** A split has to be at least this much faster than the estimate to keep being used. */
    private static final double MIN_SPEEDUP = 1.15D;

    /** How often a size class that stopped paying off is sampled again, in call count. */
    private static final int EXPLORE_MASK = 63;

    /** One counter per power of two of work, so cost is learned per size class. */
    private static final int BUCKETS = 48;

    private final String name;
    private final Bucket[] buckets = new Bucket[BUCKETS];
    private final AtomicInteger skipped = new AtomicInteger();

    private volatile double nsPerUnit = -1.0D;
    private volatile int forced = 0;

    public ParallelSite(String name) {
        this.name = name;
        for (int i = 0; i < this.buckets.length; i++) {
            this.buckets[i] = new Bucket();
        }
    }

    public String name() {
        return this.name;
    }

    /**
     * Forces this site on ({@code mode > 0}), off ({@code mode < 0}) or automatic ({@code 0}).
     *
     * @param mode The forced mode.
     */
    public void force(int mode) {
        this.forced = mode;
    }

    /** The measured cost of one unit of work, or a negative value before it is known. */
    public double nsPerUnit() {
        return this.nsPerUnit;
    }

    /**
     * The measured speedup of the last samples of this size class, or {@code NaN}.
     *
     * @param units The size of the workload being asked about.
     */
    public double speedup(long units) {
        final Bucket bucket = this.bucket(units);
        return bucket.samples == 0 ? Double.NaN : bucket.speedup;
    }

    /**
     * Whether a workload of this size should be handed to the helpers instead of run inline.
     * Until a site has been timed it always runs inline, which is what teaches it its own cost.
     *
     * @param units The estimated size of the workload.
     */
    public boolean shouldParallelize(long units) {
        final int force = this.forced;
        if (force != 0) {
            return force > 0;
        }
        final double cost = this.nsPerUnit;
        if (cost < 0.0D || cost * units < MIN_PARALLEL_NS) {
            return false;
        }
        final Bucket bucket = this.bucket(units);
        if (bucket.samples < WARMUP_SAMPLES || bucket.speedup >= MIN_SPEEDUP) {
            return true;
        }
        // A size class that stopped paying off is retried occasionally rather than never:
        // other work sharing the server thread can change how much a split is worth.
        return (this.skipped.incrementAndGet() & EXPLORE_MASK) == 0;
    }

    void recordInline(long units, long nanos) {
        if (nanos > 0L && units > 0L) {
            this.updateCost(nanos / (double) units);
        }
    }

    void recordParallel(long units, long wallNanos, long busyNanos) {
        if (wallNanos <= 0L || busyNanos <= 0L || units <= 0L) {
            return;
        }
        this.updateCost(busyNanos / (double) units);
        final Bucket bucket = this.bucket(units);
        final double sample = busyNanos / (double) wallNanos;
        bucket.speedup = bucket.samples == 0 ? sample : bucket.speedup * 0.75D + sample * 0.25D;
        bucket.samples = Math.min(bucket.samples + 1, 1 << 20);
    }

    private void updateCost(double sample) {
        final double current = this.nsPerUnit;
        this.nsPerUnit = current < 0.0D ? sample : current * 0.9D + sample * 0.1D;
    }

    private Bucket bucket(long units) {
        final int index = 63 - Long.numberOfLeadingZeros(Math.max(1L, units));
        return this.buckets[Math.min(index, this.buckets.length - 1)];
    }

    private static final class Bucket {

        volatile double speedup;
        volatile int samples;

    }

}
