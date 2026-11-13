package com.personthecat.cleancaves.world.feature;

import com.personthecat.cleancaves.world.BiomeCache;
import com.personthecat.cleancaves.world.GeneratorController;

import net.minecraft.world.World;

import java.util.Random;

public class WorldContext {

    final int[][] heightmap;
    final GeneratorController gen;
    final Random rand;
    final int chunkX;
    final int chunkZ;
    final int offsetX;
    final int offsetZ;
    final World world;

    /** Biome lookups for this chunk, shared by every feature generator. Main thread only. */
    final BiomeCache biomeCache;

    public WorldContext(int[][] heightmap, GeneratorController gen, Random rand, int chunkX, int chunkZ, World world,
            BiomeCache biomeCache) {
        this.heightmap = heightmap;
        this.gen = gen;
        this.rand = rand;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.offsetX = chunkX * 16 + 8;
        this.offsetZ = chunkZ * 16 + 8;
        this.world = world;
        this.biomeCache = biomeCache;
    }

}
