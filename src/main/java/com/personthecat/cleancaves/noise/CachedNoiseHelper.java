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

    /** The lowest block coordinates of the area the caches currently describe. */
    private static int originX;
    private static int originZ;
    private static boolean active;

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
     * It is also important to understand that this is <em>not a thread-safe implementation
     * </em>. Additional work would be needed to ensure that concurrent write operations do
     * not corrupt the data. This feature is left out here because concurrent writes never
     * have the chance to occur.
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
