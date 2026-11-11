package com.personthecat.cleancaves.world.generator;

import com.personthecat.cleancaves.CleanCaves;
import com.personthecat.cleancaves.config.ConfigFile;
import com.personthecat.cleancaves.noise.CachedNoiseHelper;
import com.personthecat.cleancaves.world.BiomeSearch;
import com.personthecat.cleancaves.world.GeneratorController;
import com.personthecat.cleancaves.world.HeightMapLocator;
import org.apache.commons.lang3.ArrayUtils;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;

import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class EarlyCaveHook extends MapGenBase {

    @Nullable MapGenBase priorCaves;

    public EarlyCaveHook(@Nullable MapGenBase priorCaves) {
        this.priorCaves = priorCaves;
    }

    @Override
    public void generate(World world, int x, int z, ChunkPrimer primer) {
        if (ConfigFile.otherGeneratorEnabled && this.priorCaves != null) {
            this.priorCaves.generate(world, x, z, primer);
        }
        // Don't really have a good way to access this without writing the game myself.
        final Map<String, GeneratorController> generators = CleanCaves.instance.loadGenerators(world);
        final int[][] heightmap = ArrayUtils.contains(ConfigFile.heightMapDims, world.provider.getDimension())
                ? HeightMapLocator.getHeightFromPrimer(primer)
                : HeightMapLocator.FAUX_MAP;

        final BiomeSearch biomes = BiomeSearch.in(world, x, z);
        final PrimerContext ctx = new PrimerContext(biomes, heightmap, world, x, z, primer);
        ctx.world.rand.setSeed(ctx.world.getSeed());
        for (GeneratorController generator : generators.values()) {
            generator.earlyGenerate(ctx);
            generator.mapGenerate(ctx);
        }
        CachedNoiseHelper.resetAll();
    }

}
