package com.personthecat.cleancaves.world.generator;

import com.personthecat.cleancaves.util.XoRoShiRo;
import com.personthecat.cleancaves.world.BiomeSearch;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

public class PrimerContext {

    /** The multipliers vanilla uses to give every chunk its own seed. */
    private static final long SEED_X = 341873128712L;
    private static final long SEED_Z = 132897987541L;

    final BiomeSearch biomes;
    final int[][] heightmap;
    final World world;
    final Random localRand;
    final int chunkX;
    final int chunkZ;
    final int offsetX;
    final int offsetZ;
    final ChunkPrimer primer;

    public PrimerContext(BiomeSearch biomes, int[][] heightmap, World world, int chunkX, int chunkZ,
            ChunkPrimer primer) {
        this.biomes = biomes;
        this.heightmap = heightmap;
        this.world = world;
        this.localRand = new XoRoShiRo(chunkSeed(world.getSeed(), chunkX, chunkZ));
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.offsetX = chunkX * 16;
        this.offsetZ = chunkZ * 16;
        this.primer = primer;
    }

    /**
     * Derives a seed for one chunk. The coordinates are weighted rather than XORed together, because
     * {@code x ^ z} is the same for (a, b) and (b, a) and is zero along the whole diagonal, which
     * would hand all of those chunks the same random stream.
     */
    public static long chunkSeed(long seed, int chunkX, int chunkZ) {
        return seed ^ (chunkX * SEED_X + chunkZ * SEED_Z);
    }

}
