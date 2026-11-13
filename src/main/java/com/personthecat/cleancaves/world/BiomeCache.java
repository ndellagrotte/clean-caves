package com.personthecat.cleancaves.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
 * Memoizes the biome lookups made while generating one chunk. Every generator used to ask the
 * world or its biome provider for the same coordinates again -- a stone-layer preset with six
 * layers asked for each of the chunk's 256 columns six times, and every feature asked for its
 * corners and neighbours. The answers cannot change during one chunk's generation, so they are
 * kept for the duration of that chunk.
 *
 * <p>{@link World#getBiome} and {@link net.minecraft.world.biome.BiomeProvider#getBiome} are kept
 * apart: the former prefers the loaded chunk's biome array while the latter always goes through
 * the provider, and the caching must not change which of the two answers a caller sees.
 *
 * <p>This cache is for the chunk-generation thread only. It is deliberately not concurrent.
 */
public final class BiomeCache {

    private final World world;

    /** Answers from {@link World#getBiome}. */
    private final Long2ObjectMap<Biome> blockBiomes = new Long2ObjectOpenHashMap<>();

    /** Answers from {@link net.minecraft.world.biome.BiomeProvider#getBiome}. */
    private final Long2ObjectMap<Biome> providerBiomes = new Long2ObjectOpenHashMap<>();

    public BiomeCache(World world) {
        this.world = world;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /** The same value {@code world.getBiome(new BlockPos(x, 0, z))} would report, memoized. */
    public Biome getBiome(int x, int z) {
        final long key = key(x, z);
        if (this.blockBiomes.containsKey(key)) {
            return this.blockBiomes.get(key);
        }
        final Biome biome = this.world.getBiome(new BlockPos(x, 0, z));
        this.blockBiomes.put(key, biome);
        return biome;
    }

    /** The same value the biome provider would report for {@code (x, 0, z)}, memoized. */
    public Biome getProviderBiome(int x, int z) {
        final long key = key(x, z);
        if (this.providerBiomes.containsKey(key)) {
            return this.providerBiomes.get(key);
        }
        final Biome biome = this.world.getBiomeProvider().getBiome(new BlockPos(x, 0, z));
        this.providerBiomes.put(key, biome);
        return biome;
    }

}
