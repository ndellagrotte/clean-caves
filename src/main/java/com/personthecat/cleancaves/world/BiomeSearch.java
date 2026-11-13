package com.personthecat.cleancaves.world;

import com.personthecat.cleancaves.config.ConfigFile;
import com.personthecat.cleancaves.util.Lazy;

import net.minecraft.world.biome.Biome;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * The biomes in and around a chunk. Both arrays are lazy because generators frequently decide
 * against spawning before they ever need to look at a biome.
 */
public final class BiomeSearch {

    public final Lazy<Biome[]> current;
    public final Lazy<Data[]> surrounding;

    private BiomeSearch(Lazy<Biome[]> current, Lazy<Data[]> surrounding) {
        this.current = current;
        this.surrounding = surrounding;
    }

    /** A public accessor to calculate the current biome array size. */
    public static int size() {
        final int d = ConfigFile.biomeRange * 2 + 1;
        return d * d;
    }

    /** Acquires biomes at the four corners of this chunk. */
    public static BiomeSearch in(BiomeCache cache, int x, int z) {
        final Lazy<Biome[]> current = Lazy.of(() -> inner(cache, x, z));
        final Lazy<Data[]> surrounding = Lazy.of(() -> outer(cache, x, z, ConfigFile.biomeRange));
        return new BiomeSearch(current, surrounding);
    }

    /** Accumulates a list of biomes at the four corners of this chunk. */
    private static Biome[] inner(BiomeCache cache, int x, int z) {
        final int actualX = x << 4;
        final int actualZ = z << 4;
        // This is only used for early generators, at which point the current
        // chunk does not yet exist. As a result, this is more direct.
        final List<Biome> biomes = Arrays.asList(cache.getProviderBiome(actualX + 1, actualZ + 1),
                cache.getProviderBiome(actualX + 1, actualZ + 14), cache.getProviderBiome(actualX + 14, actualZ + 1),
                cache.getProviderBiome(actualX + 14, actualZ + 14));
        // Remove redundant entries.
        return new HashSet<>(biomes).toArray(new Biome[0]);
    }

    /** Checks outward in a range of <code>r</code> for surrounding center biomes. */
    private static Data[] outer(BiomeCache cache, int x, int z, int r) {
        final int d = r * 2 + 1;
        final Data[] biomes = new Data[d * d];
        int index = 0;
        for (int cx = x - r; cx <= x + r; cx++) {
            for (int cz = z - r; cz <= z + r; cz++) {
                biomes[index++] = Data.create(cache, cx, cz);
            }
        }
        return biomes;
    }

    /** Checks the surrounding biomes for the first match when given a predicate. */
    public boolean anyMatches(Predicate<Biome> predicate) {
        for (Biome b : current.get()) {
            if (predicate.test(b)) {
                return true;
            }
        }
        return false;
    }

    public record Data(Biome biome, int chunkX, int chunkZ, int centerX, int centerZ) {

        private static Data create(BiomeCache cache, int chunkX, int chunkZ) {
            final int centerX = (chunkX << 4) + 8;
            final int centerZ = (chunkZ << 4) + 8;
            final Biome biome = cache.getProviderBiome(centerX, centerZ);
            return new Data(biome, chunkX, chunkZ, centerX, centerZ);
        }

    }

}
