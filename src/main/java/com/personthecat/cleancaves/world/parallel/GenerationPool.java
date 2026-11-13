package com.personthecat.cleancaves.world.parallel;

import com.personthecat.cleancaves.Reference;
import com.personthecat.cleancaves.config.ConfigFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

/**
 * Helper threads for the pure parts of cave generation. Carving itself stays on the thread that
 * asked for the chunk -- only work whose result does not depend on the order it is computed in is
 * handed out, and the results are always consumed in the original order.
 *
 * <p>The design is modelled on CaveSpeedThing by ChristKind and NoiseThreader by fonnymunkey,
 * which do the same for Better Caves. See the README for the references.
 */
public final class GenerationPool {

    private static final Logger log = LogManager.getLogger(Reference.MOD_NAME);

    /** Columns of a chunk, computed one generator column at a time. */
    public static final ParallelSite LAYER_COLUMNS = new ParallelSite("generation.layers");

    /** Columns of a chunk for cavern carving, including the noise that decides which blocks go. */
    public static final ParallelSite CAVERN_COLUMNS = new ParallelSite("generation.caverns");

    /** Columns of a chunk for cluster placement. */
    public static final ParallelSite CLUSTER_COLUMNS = new ParallelSite("generation.clusters");

    /** The position lists of a tunnel, ravine or room segment. */
    public static final ParallelSite SPHERE_FILL = new ParallelSite("generation.spheres");

    /**
     * The widest a single piece of work is split, in bands. Keeps the merges cheap. One more than
     * the automatic helper count, because the server thread fills one band itself.
     */
    public static final int MAX_BANDS = 9;

    private static final Object LOCK = new Object();
    private static final AtomicInteger THREAD_ID = new AtomicInteger();

    private static volatile ThreadPoolExecutor pool;
    private static volatile int helpers = -1;

    private GenerationPool() { }

    /**
     * How many helper threads this session uses. {@code 0} disables parallel generation. Resolved
     * from {@link ConfigFile#maxGeneratorThreads} once, then reused.
     */
    public static int helperCount() {
        int value = helpers;
        if (value < 0) {
            synchronized (LOCK) {
                if (helpers < 0) {
                    helpers = resolveHelperCount();
                }
                value = helpers;
            }
        }
        return value;
    }

    /** Whether helper threads are available and enabled. */
    public static boolean enabled() {
        return ConfigFile.parallelGeneration && helperCount() > 0;
    }

    /** The pool, or {@code null} when parallel generation is disabled or already shut down. */
    public static ThreadPoolExecutor executor() {
        if (!enabled()) {
            return null;
        }
        ThreadPoolExecutor current = pool;
        if (current == null || current.isShutdown()) {
            synchronized (LOCK) {
                current = pool;
                if (current == null || current.isShutdown()) {
                    current = createPool(helperCount());
                    pool = current;
                }
            }
        }
        return current;
    }

    /**
     * How many bands a workload should be split into. {@code 1} means run it inline.
     *
     * @param site       The hot spot being measured.
     * @param units      The estimated cost of the whole workload.
     * @param maxBands   The most bands the caller can use, usually a count of columns.
     */
    public static int bandCount(ParallelSite site, long units, int maxBands) {
        if (maxBands <= 1 || !enabled() || !site.shouldParallelize(units)) {
            return 1;
        }
        return Math.max(1, Math.min(maxBands, Math.min(helperCount() + 1, MAX_BANDS)));
    }

    /**
     * Runs {@code body} for every index in {@code [0, tasks)}. The calling thread takes part and
     * helpers pull the remaining indices from one shared counter, so no thread waits on work that
     * is not already running. When the site decides not to split, the loop runs inline.
     *
     * <p>The body must be independent of every other call to the body: it may only read state that
     * is not being written, and it may only write state that no other call touches.
     *
     * @param site         The hot spot being measured.
     * @param tasks        The number of indices to run the body for.
     * @param unitsPerTask The estimated cost of one index.
     * @param body         The work for one index.
     */
    public static void forEach(ParallelSite site, int tasks, long unitsPerTask, IntConsumer body) {
        if (tasks <= 0) {
            return;
        }
        final long units = Math.max(1L, unitsPerTask) * tasks;
        final ThreadPoolExecutor executor = enabled() && site.shouldParallelize(units) ? executor() : null;
        final int workers = executor == null ? 0 : Math.min(helperCount(), tasks - 1);
        if (workers <= 0) {
            runInline(site, tasks, units, body);
            return;
        }
        runParallel(site, executor, workers, tasks, units, body);
    }

    /** Releases the helper threads. Called when the server stops so that worlds do not leak them. */
    public static void shutdown() {
        synchronized (LOCK) {
            final ThreadPoolExecutor current = pool;
            pool = null;
            if (current != null) {
                current.shutdownNow();
            }
        }
    }

    static void runInline(ParallelSite site, int tasks, long units, IntConsumer body) {
        final long start = System.nanoTime();
        for (int i = 0; i < tasks; i++) {
            body.accept(i);
        }
        site.recordInline(units, System.nanoTime() - start);
    }

    static void runParallel(ParallelSite site, ThreadPoolExecutor executor, int helperCount, int tasks, long units,
            IntConsumer body) {
        final AtomicInteger next = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final AtomicLong busyNanos = new AtomicLong();
        final Runnable work = () -> {
            final long start = System.nanoTime();
            boolean worked = false;
            int index;
            while ((index = next.getAndIncrement()) < tasks) {
                worked = true;
                try {
                    body.accept(index);
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                    next.set(tasks);
                    break;
                }
            }
            if (worked) {
                busyNanos.addAndGet(System.nanoTime() - start);
            }
        };
        final List<Future<?>> futures = new ArrayList<>(helperCount);
        final long start = System.nanoTime();
        for (int i = 0; i < helperCount; i++) {
            try {
                futures.add(executor.submit(work));
            } catch (RejectedExecutionException e) {
                break;
            }
        }
        work.run();
        final long workEnd = System.nanoTime();
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for cave generation helpers.", e);
            } catch (ExecutionException e) {
                error.compareAndSet(null, e.getCause());
            }
        }
        final long end = System.nanoTime();
        site.recordParallel(units, end - start, busyNanos.get());
        final Throwable thrown = error.get();
        if (thrown != null) {
            rethrow(thrown);
        }
        if (log.isDebugEnabled()) {
            final long wait = end - workEnd;
            log.debug("{}: {} tasks over {} helpers in {} ms (waited {} ms).", site.name(), tasks, futures.size(),
                    (end - start) / 1_000_000L, wait / 1_000_000L);
        }
    }

    private static void rethrow(Throwable thrown) {
        if (thrown instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (thrown instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException(thrown);
    }

    private static ThreadPoolExecutor createPool(int helperCount) {
        final AtomicInteger ids = THREAD_ID;
        final ThreadPoolExecutor created = new ThreadPoolExecutor(helperCount, helperCount, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> {
                    final Thread thread = new Thread(runnable, "CleanCaves-Worker-" + ids.incrementAndGet());
                    // The server may stop at any point; helper threads must never keep the JVM alive.
                    thread.setDaemon(true);
                    return thread;
                });
        created.allowCoreThreadTimeOut(true);
        log.info("Using {} helper thread(s) for parallel cave generation.", helperCount);
        return created;
    }

    private static int resolveHelperCount() {
        final int configured = ConfigFile.maxGeneratorThreads;
        final int cpus = Runtime.getRuntime().availableProcessors();
        if (configured > 0) {
            final int value = Math.min(configured, 64);
            log.info("Using {} helper thread(s) (configured, {} CPU threads).", value, cpus);
            return value;
        }
        if (cpus <= 2) {
            log.info("Only {} CPU thread(s) available, cave generation stays on the server thread.", cpus);
            return 0;
        }
        final boolean client = isClientSide();
        // Leave room for the server thread itself, the client renderer and, when installed, Pulsar.
        final int reserve = (client ? 4 : 2) + (isLoaded("pulsar") ? Math.max(1, cpus / 3) : 0);
        // Matches the automatic ceiling of the reference mod. The server thread always works too.
        final int value = Math.max(1, Math.min(cpus - reserve, MAX_BANDS - 1));
        log.info("Using {} helper thread(s) (auto: {} CPU threads, {} side, Pulsar present: {}).", value, cpus,
                client ? "client" : "server", isLoaded("pulsar"));
        return value;
    }

    private static boolean isClientSide() {
        try {
            return FMLCommonHandler.instance().getSide() == Side.CLIENT;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isLoaded(String modId) {
        try {
            return Loader.isModLoaded(modId);
        } catch (Throwable t) {
            return false;
        }
    }

}
