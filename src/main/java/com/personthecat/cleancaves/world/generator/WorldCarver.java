package com.personthecat.cleancaves.world.generator;

import com.personthecat.cleancaves.data.ConditionSettings;
import com.personthecat.cleancaves.data.DecoratorSettings;
import com.personthecat.cleancaves.data.PondSettings;
import com.personthecat.cleancaves.model.ConfiguredCaveBlock;
import com.personthecat.cleancaves.model.ConfiguredPond;
import com.personthecat.cleancaves.model.ConfiguredShell;
import com.personthecat.cleancaves.model.ConfiguredWallDecorator;
import com.personthecat.cleancaves.model.Decorators;
import com.personthecat.cleancaves.util.PositionFlags;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.List;
import java.util.Random;

/** An early world generator supporting cave block replacements and surface decorations. */
public abstract class WorldCarver extends BasicGenerator {

    protected final Decorators decorators;
    protected final int maxPondDepth;

    public WorldCarver(ConditionSettings conditions, DecoratorSettings decorators, World world) {
        super(conditions, world);
        this.decorators = Decorators.compile(decorators, world);
        this.maxPondDepth = getMaxPondDepth(decorators.ponds());
    }

    private static int getMaxPondDepth(List<PondSettings> ponds) {
        int max = 0;
        for (PondSettings pond : ponds) {
            max = Math.max(pond.depth(), max);
        }
        return max;
    }

    private static IBlockState[] getSurrounding(World world, ChunkPrimer primer, int x, int y, int z, int absX,
            int absZ) {
        return new IBlockState[] {
                x == 0 ? getBlock(world, absX - 1, y, absZ) : primer.getBlockState(x - 1, y, z),
                x == 15 ? getBlock(world, absX + 1, y, absZ) : primer.getBlockState(x + 1, y, z),
                z == 0 ? getBlock(world, absX, y, absZ - 1) : primer.getBlockState(x, y, z - 1),
                z == 15 ? getBlock(world, absX, y, absZ + 1) : primer.getBlockState(x, y, z + 1)
        };
    }

    private static IBlockState getBlock(World world, int absX, int y, int absZ) {
        final BlockPos pos = new BlockPos(absX, y, absZ);
        if (world.isBlockLoaded(pos)) {
            return world.getBlockState(pos);
        }
        return null;
    }

    private static boolean anySolid(IBlockState[] states) {
        for (IBlockState state : states) {
            if (state != null && state.getMaterial().isSolid()) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyAir(IBlockState[] states) {
        for (IBlockState state : states) {
            if (state != null && Blocks.AIR.equals(state.getBlock())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fills the floor below (x, y, z) with a pond, one layer at a time, stopping at the bottom of the
     * world or at the first block that fails the integrity roll.
     *
     * @return The number of layers that were filled.
     */
    private static int placeCountPond(ConfiguredPond pond, ChunkPrimer primer, Random rand, int x, int y, int z) {
        final int depth = Math.min(pond.cfg.depth(), y);
        if (pond.cfg.integrity() == 1.0 && pond.cfg.states().size() > 0) {
            final IBlockState state = pond.cfg.states().get(0);
            for (int i = 1; i <= depth; i++) {
                primer.setBlockState(x, y - i, z, state);
            }
            return depth;
        }
        for (int i = 1; i <= depth; i++) {
            for (IBlockState state : pond.cfg.states()) {
                if (rand.nextFloat() <= pond.cfg.integrity()) {
                    primer.setBlockState(x, y - i, z, state);
                } else {
                    return i - 1;
                }
            }
        }
        return depth;
    }

    // Avoids redundant noise calculations, if applicable.
    private static boolean decorateAll(List<ConfiguredWallDecorator> all, ChunkPrimer primer, Random rand, int x, int y,
            int z, int cx, int cz) {
        for (ConfiguredWallDecorator decorator : all) {
            if (decorator.canGenerate(rand, x, y, z, cx, cz)) {
                if (y > 0 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y - 1, z)) {
                    return true;
                }
                if (y < 255 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y + 1, z)) {
                    return true;
                }
                if (x > 0 && checkPlaceWall(decorator, primer, rand, x, y, z, x - 1, y, z)) {
                    return true;
                }
                if (x < 15 && checkPlaceWall(decorator, primer, rand, x, y, z, x + 1, y, z)) {
                    return true;
                }
                if (z > 0 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y, z - 1)) {
                    return true;
                }
                if (z < 15 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y, z + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean decorateSide(List<ConfiguredWallDecorator> side, ChunkPrimer primer, Random rand, int x,
            int y, int z, int cx, int cz) {
        for (ConfiguredWallDecorator decorator : side) {
            if (decorator.canGenerate(rand, x, y, z, cx, cz)) {
                if (x > 0 && checkPlaceWall(decorator, primer, rand, x, y, z, x - 1, y, z)) {
                    return true;
                }
                if (x < 15 && checkPlaceWall(decorator, primer, rand, x, y, z, x + 1, y, z)) {
                    return true;
                }
                if (z > 0 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y, z - 1)) {
                    return true;
                }
                if (z < 15 && checkPlaceWall(decorator, primer, rand, x, y, z, x, y, z + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkPlaceWall(ConfiguredWallDecorator decorator, ChunkPrimer primer, Random rand, int x,
            int y, int z, int offX, int offY, int offZ) {
        if (decorator.matchesBlock(primer.getBlockState(offX, offY, offZ))) {
            return placeWall(decorator, primer, rand, x, y, z, offX, offY, offZ);
        }
        return false;
    }

    private static boolean decorate(List<ConfiguredWallDecorator> decorators, ChunkPrimer primer, Random rand, int x,
            int y, int z, int offX, int offY, int offZ, int cx, int cz) {
        for (ConfiguredWallDecorator decorator : decorators) {
            final IBlockState candidate = primer.getBlockState(offX, offY, offZ);
            if (decorator.canGenerate(rand, candidate, x, y, z, cx, cz)) {
                if (placeWall(decorator, primer, rand, x, y, z, offX, offY, offZ)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Returns true if the current block is replaced, not the wall.
    private static boolean placeWall(ConfiguredWallDecorator decorator, ChunkPrimer primer, Random rand, int x, int y,
            int z, int offX, int offY, int offZ) {
        for (IBlockState state : decorator.cfg.states()) {
            if (rand.nextFloat() <= decorator.cfg.integrity()) {
                if (decorator.decidePlace(state, primer, x, y, z, offX, offY, offZ)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns whether the generator has any surface decorators. */
    protected boolean hasWallDecorators() {
        return this.decorators.wallMap.containsAny();
    }

    /** Returns whether the generator has any pond decorators. */
    protected boolean hasPonds() {
        return !this.decorators.ponds.isEmpty();
    }

    /** Returns whether the generator has any shell decorators. */
    protected boolean hasShell() {
        return !this.decorators.shell.decorators.isEmpty();
    }

    /** Spawns whichever cave block is valid at this location, or else air. */
    protected void replaceBlock(Random rand, ChunkPrimer primer, int x, int y, int z, int chunkX, int chunkZ) {
        if (this.decorators.canReplace.test(primer.getBlockState(x, y, z))) {
            for (ConfiguredCaveBlock block : this.decorators.caveBlocks) {
                if (block.canGenerate(x, y, z, chunkX, chunkZ)) {
                    for (IBlockState state : block.cfg.states()) {
                        if (rand.nextFloat() <= block.cfg.integrity()) {
                            primer.setBlockState(x, y, z, state);
                            return;
                        }
                    }
                }
            }
            primer.setBlockState(x, y, z, BLK_AIR);
        }
    }

    protected void decorateAll(PositionFlags positions, Random rand, World world, ChunkPrimer primer, int cx, int cz) {
        if (this.hasPonds()) {
            this.generatePond(positions, rand, world, primer, cx, cz);
        }
        if (this.hasWallDecorators()) {
            this.generateWall(positions, rand, primer, cx, cz);
        }
    }

    protected void generatePond(PositionFlags positions, Random rand, World world, ChunkPrimer primer, int cx, int cz) {
        positions.forEach((x, y, z) -> evaluatePond(world, primer, rand, x, y, z, cx, cz));
    }

    /** Spawns blocks from the shell decorator settings for a single coordinate. */
    protected void generateShell(Random rand, ChunkPrimer primer, int x, int y, int z, int cy, int chunkX, int chunkZ) {
        for (ConfiguredShell.Decorator shell : this.decorators.shell.decorators) {
            if (shell.cfg.height().contains(cy)) {
                final IBlockState candidate = primer.getBlockState(x, y, z);
                if (shell.matches(candidate) && shell.testNoise(x, y, z, chunkX, chunkZ)) {
                    for (IBlockState state : shell.cfg.states()) {
                        if (rand.nextFloat() <= shell.cfg.integrity()) {
                            primer.setBlockState(x, y, z, state);
                            return;
                        }
                    }
                }
            }
        }
    }

    protected void generateWall(PositionFlags positions, Random rand, ChunkPrimer primer, int cx, int cz) {
        positions.forEach((x, y, z) -> this.decorateBlock(rand, primer, x, y, z, cx, cz));
    }

    /** Conditionally replaces the current block with blocks from this generator's WallDecorators. */
    protected void decorateBlock(Random rand, ChunkPrimer primer, int x, int y, int z, int cx, int cz) {
        if (!this.decorators.wallMap.containsAny()) {
            return;
        }
        if (decorateAll(this.decorators.wallMap.all(), primer, rand, x, y, z, cx, cz)) {
            return;
        }
        if (decorateSide(this.decorators.wallMap.side(), primer, rand, x, y, z, cx, cz)) {
            return;
        }
        if (y > 0 && decorate(this.decorators.wallMap.down(), primer, rand, x, y, z, x, y - 1, z, cx, cz)) {
            return;
        }
        if (y < 255 && decorate(this.decorators.wallMap.up(), primer, rand, x, y, z, x, y + 1, z, cx, cz)) {
            return;
        }
        if (x > 0 && decorate(this.decorators.wallMap.west(), primer, rand, x, y, z, x - 1, y, z, cx, cz)) {
            return;
        }
        if (x < 15 && decorate(this.decorators.wallMap.east(), primer, rand, x, y, z, x + 1, y, z, cx, cz)) {
            return;
        }
        if (z > 0 && decorate(this.decorators.wallMap.north(), primer, rand, x, y, z, x, y, z - 1, cx, cz)) {
            return;
        }
        if (z < 15) {
            decorate(this.decorators.wallMap.south(), primer, rand, x, y, z, x, y, z + 1, cx, cz);
        }
    }

    private void evaluatePond(World world, ChunkPrimer primer, Random rand, int x, int y, int z, int cx, int cz) {
        final int absX = cx * 16 + x;
        final int absZ = cz * 16 + z;
        // There is no floor to fill below the bottom of the world.
        if (y < 1) {
            return;
        }
        final IBlockState candidate = primer.getBlockState(x, y - 1, z);
        if (Blocks.AIR.equals(candidate.getBlock())) {
            return;
        }
        if (anySolid(getSurrounding(world, primer, x, y, z, absX, absZ))) {
            return;
        }
        if (anyAir(getSurrounding(world, primer, x, y - 1, z, absX, absZ))) {
            return;
        }
        // The depth already filled. A later pond only gets to overwrite an earlier one when it is
        // at least as deep, so that a shallow pond never leaves a deeper one half replaced.
        int d = 1;
        for (ConfiguredPond pond : this.decorators.ponds) {
            if (pond.cfg.depth() >= d && pond.canGenerate(rand, candidate, x, y, z, cx, cz)) {
                d = Math.max(d, placeCountPond(pond, primer, rand, x, y, z));
            }
        }
    }

}
