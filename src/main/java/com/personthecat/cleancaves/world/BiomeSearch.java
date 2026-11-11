package com.personthecat.cleancaves.world;

import com.personthecat.cleancaves.config.ConfigFile;
import com.personthecat.cleancaves.util.Lazy;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

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
    public static BiomeSearch in(World world, int x, int z) {
        final Lazy<Biome[]> current = Lazy.of(() -> inner(world, x, z));
        final Lazy<Data[]> surrounding = Lazy.of(() -> outer(world, x, z, ConfigFile.biomeRange));
        return new BiomeSearch(current, surrounding);
    }

    /** Accumulates a list of biomes at the four corners of this chunk. */
    private static Biome[] inner(World world, int x, int z) {
        final BiomeProvider provider = world.getBiomeProvider();
        final int actualX = x << 4;
        final int actualZ = z << 4;
        // This is only used for early generators, at which point the current
        // chunk does not yet exist. As a result, this is more direct.
        final List<Biome> biomes = Arrays.asList(provider.getBiome(new BlockPos(actualX + 1, 0, actualZ + 1)),
                provider.getBiome(new BlockPos(actualX + 1, 0, actualZ + 14)),
                provider.getBiome(new BlockPos(actualX + 14, 0, actualZ + 1)),
                provider.getBiome(new BlockPos(actualX + 14, 0, actualZ + 14)));
        // Remove redundant entries.
        return new HashSet<>(biomes).toArray(new Biome[0]);
    }

    /** Checks outward in a range of <code>r</code> for surrounding center biomes. */
    private static Data[] outer(World world, int x, int z, int r) {
        final int d = r * 2 + 1;
        final Data[] biomes = new Data[d * d];
        int index = 0;
        for (int cx = x - r; cx <= x + r; cx++) {
            for (int cz = z - r; cz <= z + r; cz++) {
                biomes[index++] = Data.create(world, cx, cz);
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

        private static Data create(World world, int chunkX, int chunkZ) {
            final int centerX = (chunkX << 4) + 8;
            final int centerZ = (chunkZ << 4) + 8;
            final Biome biome = world.getBiomeProvider().getBiome(new BlockPos(centerX, 0, centerZ));
            return new Data(biome, chunkX, chunkZ, centerX, centerZ);
        }

    }

}
