package com.personthecat.cleancaves.noise;

import fastnoise.FastNoise;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the noise already computed for the 16x16 column of blocks that is currently being
 * generated. A cache slot is addressed by a block's position within that area alone, so the values
 * are only meaningful for coordinates inside of it: everything else must go to the generator.
 */
public final class CachedNoiseHelper {

    /** The width of the cached area, which is one chunk. */
    private static final int SIZE = 16;

    /** Keyed by the generator itself, so that only generators producing the same noise share. */
    private static final Map<FastNoise, Cache> DATA = new ConcurrentHashMap<>();

    /**
     * The lowest block coordinates of the area the caches currently describe. Volatile because
     * helper threads read them while the server thread starts and ends an area.
     */
    private static volatile int originX;
    private static volatile int originZ;
    private static volatile boolean active;

    private CachedNoiseHelper() { }

    public static Cache getOrCreate(FastNoise generator) {
        return DATA.computeIfAbsent(generator, g -> new Cache());
    }

    /**
     * Starts caching for a new area, discarding whatever the last one left behind.
     *
     * @param minX The lowest block x coordinate of the area.
     * @param minZ The lowest block z coordinate of the area.
     */
    public static void begin(int minX, int minZ) {
        DATA.values().forEach(Cache::reset);
        originX = minX;
        originZ = minZ;
        active = true;
    }

    /** Stops caching until the next area begins. */
    public static void end() {
        active = false;
    }

    public static void removeAll() {
        DATA.clear();
        active = false;
    }

    /** Whether noise at this block belongs to the area the caches currently describe. */
    static boolean covers(int x, int z) {
        return active && x >= originX && x < originX + SIZE && z >= originZ && z < originZ + SIZE;
    }

    /**
     * Callers must be careful to ensure boundaries are not exceeded.
     *
     * Several threads may read and write one cache while parallel generation is filling a chunk:
     * every thread computes the same value for a coordinate and writes it to the same slot, and a
     * read that misses a write simply computes the value again. The array is never resized or
     * cleared while generation is running--{@link #begin} resets it before any work starts and
     * {@link #end} stops caching afterwards--so there is nothing for the writes to corrupt.
     */
    public static class Cache {

        private final float[] output3 = new float[16 * 16 * 256];
        private final float[] output2 = new float[16 * 16];

        public float getNoise(int x, int y, int z) {
            return output3[x << 12 | z << 8 | y];
        }

        public float getNoise(int x, int z) {
            return output2[x << 4 | z];
        }

        public void writeNoise(int x, int y, int z, float noise) {
            output3[x << 12 | z << 8 | y] = noise;
        }

        public void writeNoise(int x, int z, float noise) {
            output2[x << 4 | z] = noise;
        }

        private void reset() {
            Arrays.fill(output3, 0F);
            Arrays.fill(output2, 0F);
        }

    }

}
