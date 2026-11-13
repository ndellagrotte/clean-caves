package com.personthecat.cleancaves.world.generator;

import com.personthecat.cleancaves.data.LayerSettings;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.BlockMask;
import com.personthecat.cleancaves.world.parallel.GenerationPool;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class LayerGenerator extends BasicGenerator {

    /** The number of columns in a chunk. */
    private static final int COLUMNS = 16 * 16;

    /** A rough measure of how much work one column is, for the workload detector. */
    private static final long UNITS_PER_COLUMN = 96L;

    private final LayerSettings cfg;

    /**
     * Per-column results, reused between chunks. Helper threads write one entry each, so the main
     * thread always gets to walk the same coordinates in the same order it would have without them.
     */
    private final Range[] columns = new Range[COLUMNS];
    private final BlockMask[] masks = new BlockMask[COLUMNS];
    private final boolean[] valid = new boolean[COLUMNS];

    public LayerGenerator(LayerSettings cfg, World world) {
        super(cfg.conditions(), world);
        this.cfg = cfg;
        for (int i = 0; i < COLUMNS; i++) {
            this.masks[i] = new BlockMask();
        }
    }

    @Override
    public void generate(PrimerContext ctx) {
        final int dim = ctx.world.provider.getDimension();
        if (conditions.dimensions.test(dim)) {
            generateChecked(ctx);
        }
    }

    @Override
    protected void generateChecked(PrimerContext ctx) {
        // Biome lookups go through the world's biome provider, which is not safe to touch from a
        // helper thread. They are cheap compared to the noise, so they are read here first.
        final Biome[] biomes = new Biome[COLUMNS];
        for (int x = 0; x < 16; x++) {
            final int actualX = x + (ctx.chunkX * 16);
            for (int z = 0; z < 16; z++) {
                final int actualZ = z + (ctx.chunkZ * 16);
                biomes[x << 4 | z] = ctx.biomeCache.getBiome(actualX, actualZ);
                this.valid[x << 4 | z] = false;
                this.masks[x << 4 | z].clear();
            }
        }
        GenerationPool.forEach(GenerationPool.LAYER_COLUMNS, COLUMNS, UNITS_PER_COLUMN,
                i -> this.prepareColumn(ctx, biomes, i));
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                this.writeColumn(ctx, x, z);
            }
        }
    }

    /** Computes everything about one column that does not depend on other columns: pure noise. */
    private void prepareColumn(PrimerContext ctx, Biome[] biomes, int i) {
        final int x = i >> 4;
        final int z = i & 15;
        if (!this.conditions.biomes.test(biomes[i])) {
            return;
        }
        final int actualX = x + (ctx.chunkX * 16);
        final int actualZ = z + (ctx.chunkZ * 16);
        if (!this.conditions.region.GetBoolean(actualX, actualZ)) {
            return;
        }
        final Range column = this.conditions.getColumn(actualX, actualZ);
        this.columns[i] = column;
        this.valid[i] = true;
        if (!this.conditions.noise.GetBoolean(actualX, actualZ)) {
            return;
        }
        final BlockMask mask = this.masks[i];
        for (int y : column) {
            if (this.cfg.matchers().contains(ctx.primer.getBlockState(x, y, z))) {
                mask.set(y);
            }
        }
    }

    /** Applies one prepared column. No noise is evaluated here. */
    private void writeColumn(PrimerContext ctx, int x, int z) {
        final int i = x << 4 | z;
        if (!this.valid[i]) {
            return;
        }
        final BlockMask mask = this.masks[i];
        if (mask.isEmpty()) {
            return;
        }
        for (int y : this.columns[i]) {
            if (mask.get(y)) {
                ctx.primer.setBlockState(x, y, z, this.cfg.state());
            }
        }
    }

}
