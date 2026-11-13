package com.personthecat.cleancaves.world.generator;

import com.personthecat.cleancaves.config.ConfigFile;
import com.personthecat.cleancaves.data.ConditionSettings;
import com.personthecat.cleancaves.data.DecoratorSettings;
import com.personthecat.cleancaves.model.ConfiguredCaveBlock;
import com.personthecat.cleancaves.model.PrimerData;
import com.personthecat.cleancaves.util.PositionFlags;
import com.personthecat.cleancaves.util.XoRoShiRo;
import com.personthecat.cleancaves.world.BiomeSearch;
import com.personthecat.cleancaves.world.parallel.GenerationPool;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class MapGenerator extends WorldCarver {

    private static final IBlockState BLK_WATER = Blocks.WATER.getDefaultState();

    /** The vertical distance to the nearest water source block that can be ignored. */
    private static final int WATER_WIGGLE_ROOM = 7;

    protected final List<BlockPos> invalidChunks = new ArrayList<>(BiomeSearch.size());
    private final SphereData sphere = new SphereData();
    protected final Random rand = new XoRoShiRo(0L);
    private final boolean testWater;

    /** Per-band position lists used when a sphere is filled on several threads. Allocated lazily. */
    private SphereData[] bands;

    public MapGenerator(ConditionSettings conditions, DecoratorSettings decorators, World world, boolean testWater) {
        super(conditions, decorators, world);
        this.testWater = testWater;
    }

    @Override
    public final void generate(PrimerContext ctx) {
        if (this.conditions.dimensions.test(ctx.world.provider.getDimension())) {
            if (this.conditions.hasBiomes) {
                if (ctx.biomes.anyMatches(this.conditions.biomes)) {
                    this.fillInvalidChunks(ctx.biomes);
                    this.generateChecked(ctx);
                    this.invalidChunks.clear();
                }
            } else if (this.conditions.hasRegion) {
                if (this.conditions.region.GetBoolean(ctx.offsetX, ctx.offsetZ)) {
                    this.fillInvalidChunks(ctx.chunkX, ctx.chunkZ);
                    this.generateChecked(ctx);
                    this.invalidChunks.clear();
                }
            } else {
                this.generateChecked(ctx);
            }
        }
    }

    private void fillInvalidChunks(BiomeSearch biomes) {
        for (BiomeSearch.Data d : biomes.surrounding.get()) {
            if (!(this.conditions.biomes.test(d.biome())
                    && this.conditions.region.GetBoolean(d.centerX(), d.centerZ()))) {
                this.invalidChunks.add(new BlockPos(d.centerX(), 0, d.centerZ()));
            }
        }
    }

    private void fillInvalidChunks(int chunkX, int chunkZ) {
        final int range = ConfigFile.biomeRange;
        for (int cx = chunkX - range; cx <= chunkX + range; cx++) {
            for (int cz = chunkZ - range; cz <= chunkZ + range; cz++) {
                final int centerX = cx * 16 + 8;
                final int centerZ = cz * 16 + 8;
                if (!this.conditions.region.GetBoolean(centerX, centerZ)) {
                    this.invalidChunks.add(new BlockPos(centerX, 0, centerZ));
                }
            }
        }
    }

    @Override
    protected final void generateChecked(PrimerContext ctx) {
        final int range = ConfigFile.mapRange;
        this.rand.setSeed(ctx.world.getSeed());
        final long xMask = this.rand.nextLong();
        final long zMask = this.rand.nextLong();

        for (int destX = ctx.chunkX - range; destX <= ctx.chunkX + range; destX++) {
            for (int destZ = ctx.chunkZ - range; destZ <= ctx.chunkZ + range; destZ++) {
                long hashX = (long) destX * xMask;
                long hashZ = (long) destZ * zMask;
                this.rand.setSeed(hashX ^ hashZ ^ ctx.world.getSeed());
                this.mapGenerate(new MapGenerationContext(ctx, rand, destX, destZ));
            }
        }
    }

    protected abstract void mapGenerate(MapGenerationContext ctx);

    protected double getNearestBorder(int x, int z) {
        double shortestDistance = Double.MAX_VALUE;

        for (BlockPos invalid : this.invalidChunks) {
            final double sum = Math.pow(x - invalid.getX(), 2) + Math.pow(z - invalid.getZ(), 2);
            final double distance = Math.sqrt(sum);

            shortestDistance = Math.min(distance, shortestDistance);
        }
        return shortestDistance;
    }

    protected final void generateSphere(PrimerData data, World world, Random rand, double x, double y, double z,
            double radXZ, double radY, double roXZ, double roY) {
        final int miX = limitXZ(MathHelper.floor(x - roXZ) - data.absX() - 1);
        final int maX = limitXZ(MathHelper.floor(x + roXZ) - data.absX() + 1);
        final int miY = limitY(MathHelper.floor(y - roY) - 1);
        final int maY = limitY(MathHelper.floor(y + roY) + 1);
        final int miZ = limitXZ(MathHelper.floor(z - roXZ) - data.absZ() - 1);
        final int maZ = limitXZ(MathHelper.floor(z + roXZ) - data.absZ() + 1);

        this.sphere.reset();
        this.sphere.grow(maX - miX, maY - miY, maZ - miZ);

        // The choice of shape is drawn first, so splitting the fill cannot change the random stream.
        final boolean doubleFill = roXZ - radXZ != 0 && rand.nextInt(decorators.shell.cfg.sphereResolution()) == 0;
        final SphereShape shape = new SphereShape(x, y, z, data.absX(), data.absZ(), radXZ, radY, roXZ, roY);
        this.fillSphereInBands(doubleFill, shape, miX, maX, miY, maY, miZ, maZ);

        // If we need to test this section for water -> is there water?
        if (!(this.shouldTestForWater(miY, maY) && this.testForWater(data.p(), this.sphere.inner))) {
            this.generateShell(data, rand, this.sphere.shell, (int) y);
            this.replaceSphere(data, rand, this.sphere.inner);
            this.decorateAll(this.sphere.inner, rand, world, data.p(), data.chunkX(), data.chunkZ());
        }
    }

    /**
     * Fills the sphere's position lists, optionally splitting the x axis across helper threads.
     * Each band writes its own list and the lists are merged in ascending x order, which is the
     * order a single-threaded fill adds them in, so decoration sees an identical position list.
     */
    private void fillSphereInBands(boolean doubleFill, SphereShape shape, int miX, int maX, int miY, int maY, int miZ,
            int maZ) {
        final int span = maX - miX;
        final long volume = (long) span * (maY - miY) * (maZ - miZ);
        final int bandCount = GenerationPool.bandCount(GenerationPool.SPHERE_FILL, volume, span);
        if (bandCount <= 1) {
            GenerationPool.forEach(GenerationPool.SPHERE_FILL, 1, volume,
                    i -> this.fillSphereRange(doubleFill, this.sphere, shape, miX, maX, miY, maY, miZ, maZ));
            return;
        }
        final SphereData[] parts = this.bandBuffers();
        for (int i = 0; i < bandCount; i++) {
            parts[i].reset();
        }
        GenerationPool.forEach(GenerationPool.SPHERE_FILL, bandCount, volume / bandCount, i -> {
            final int from = miX + (int) ((long) span * i / bandCount);
            final int to = miX + (int) ((long) span * (i + 1) / bandCount);
            final SphereData part = parts[i];
            part.grow(to - from, maY - miY, maZ - miZ);
            this.fillSphereRange(doubleFill, part, shape, from, to, miY, maY, miZ, maZ);
        });
        for (int i = 0; i < bandCount; i++) {
            this.sphere.inner.addAll(parts[i].inner);
            this.sphere.shell.addAll(parts[i].shell);
        }
    }

    /** The single-threaded fill of an x range, shared by the inline and banded paths. */
    private void fillSphereRange(boolean doubleFill, SphereData target, SphereShape shape, int miX, int maX, int miY,
            int maY, int miZ, int maZ) {
        if (doubleFill) {
            this.fillDouble(target, shape.x(), shape.y(), shape.z(), shape.absX(), shape.absZ(), shape.radXZ(),
                    shape.radY(), shape.roXZ(), shape.roY(), miX, maX, miY, maY, miZ, maZ);
        } else {
            this.fillSphere(target, shape.x(), shape.y(), shape.z(), shape.absX(), shape.absZ(), shape.radXZ(),
                    shape.radY(), miX, maX, miY, maY, miZ, maZ);
        }
    }

    /** The geometry of one sphere segment: where it sits and how large it is. */
    private record SphereShape(double x,
            double y,
            double z,
            int absX,
            int absZ,
            double radXZ,
            double radY,
            double roXZ,
            double roY) { }

    private SphereData[] bandBuffers() {
        if (this.bands == null) {
            this.bands = new SphereData[GenerationPool.MAX_BANDS];
            for (int i = 0; i < this.bands.length; i++) {
                this.bands[i] = new SphereData();
            }
        }
        return this.bands;
    }

    /** Makes sure the resulting value stays within chunk bounds. */
    private static int limitXZ(int xz) {
        return xz < 0 ? 0 : Math.min(xz, 16);
    }

    /** Makes sure the resulting value stays between y = 1 & y = 248 */
    private static int limitY(int y) {
        return y < 1 ? 1 : Math.min(y, 248);
    }

    /** Provides reusable instructions on where to place and decorate blocks. */
    protected abstract void fillSphere(SphereData sphere, double cx, double cy, double cz, int absX, int absZ,
            double radXZ, double radY, int miX, int maX, int miY, int maY, int miZ, int maZ);

    /** Variant of #fillSphere which includes an outer shell. This is more expensive. */
    protected abstract void fillDouble(SphereData sphere, double cx, double cy, double cz, int absX, int absZ,
            double radXZ, double radY, double roXZ, double roY, int miX, int maX, int miY, int maY, int miZ, int maZ);

    /** Calculates the maximum distance for this tunnel, if needed. */
    protected int getDistance(Random rand, int input) {
        if (input <= 0) {
            return 112 - rand.nextInt(28);
        }
        return input;
    }

    /**
     * Returns whether a test should be run to determine whether water is
     * found and stop generating.
     */
    private boolean shouldTestForWater(int miY, int maY) {
        if (!this.testWater) {
            return false;
        }
        for (ConfiguredCaveBlock block : decorators.caveBlocks) {
            if (block.cfg.states().contains(BLK_WATER)) {
                if (maY <= block.cfg.height().max() + WATER_WIGGLE_ROOM
                        && miY >= block.cfg.height().min() - WATER_WIGGLE_ROOM) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Determines whether any water exists in the current section. */
    protected boolean testForWater(ChunkPrimer primer, PositionFlags sphere) {
        return sphere.anyMatches((x, y, z) -> primer.getBlockState(x, y, z).equals(BLK_WATER));
    }

    /** Replaces all of the blocks around the current sphere, if applicable. */
    protected void generateShell(PrimerData data, Random rand, PositionFlags sphere, int cy) {
        sphere.forEach((x, y, z) -> this.generateShell(rand, data.p(), x, y, z, cy, data.chunkX(), data.chunkZ()));
    }

    /** Replaces all blocks inside of this section. */
    protected void replaceSphere(PrimerData data, Random rand, PositionFlags sphere) {
        sphere.forEach((x, y, z) -> this.replaceBlock(rand, data.p(), x, y, z, data.chunkX(), data.chunkZ()));
    }

    /** Decorates all blocks inside of this section. */
    protected void decorateSphere(PrimerData data, Random rand, PositionFlags sphere) {
        sphere.forEach((x, y, z) -> this.decorateBlock(rand, data.p(), x, y, z, data.chunkX(), data.chunkZ()));
    }

    protected static final class MapGenerationContext {

        protected final int[][] heightmap;
        protected final World world;
        protected final Random rand;
        protected final int destChunkX;
        protected final int destChunkZ;
        protected final int chunkX;
        protected final int chunkZ;
        protected final int offsetX;
        protected final int offsetZ;
        protected final ChunkPrimer primer;

        private MapGenerationContext(PrimerContext ctx, Random rand, int destChunkX, int destChunkZ) {
            this.heightmap = ctx.heightmap;
            this.world = ctx.world;
            this.rand = rand;
            this.destChunkX = destChunkX;
            this.destChunkZ = destChunkZ;
            this.chunkX = ctx.chunkX;
            this.chunkZ = ctx.chunkZ;
            this.offsetX = ctx.offsetX;
            this.offsetZ = ctx.offsetZ;
            this.primer = ctx.primer;
        }

    }

}
