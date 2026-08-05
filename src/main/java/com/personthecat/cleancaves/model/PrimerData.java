package com.personthecat.cleancaves.model;

import net.minecraft.world.chunk.ChunkPrimer;

/**
 * Consolidates positional data when generating world features via {@code MapGenBase} objects.
 * The absolute and centre coordinates are derived from the chunk coordinates.
 */
public record PrimerData(
    ChunkPrimer p,
    int chunkX,
    int chunkZ,
    int absX,
    int absZ,
    int centerX,
    int centerZ
) {
    public PrimerData(ChunkPrimer p, int chunkX, int chunkZ) {
        this(p, chunkX, chunkZ, chunkX * 16, chunkZ * 16, chunkX * 16 + 8, chunkZ * 16 + 8);
    }
}
