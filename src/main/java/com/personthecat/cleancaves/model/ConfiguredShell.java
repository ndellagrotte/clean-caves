package com.personthecat.cleancaves.model;

import com.personthecat.cleancaves.data.ShellSettings;
import com.personthecat.cleancaves.noise.DummyGenerator;
import fastnoise.FastNoise;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;

import java.util.List;

public class ConfiguredShell {

    public static final ConfiguredShell EMPTY_SHELL = new ConfiguredShell();

    public final ShellSettings cfg;
    public final double radius;
    public final float noiseThreshold;
    public final List<Decorator> decorators;

    public ConfiguredShell(ShellSettings cfg, World world) {
        this.cfg = cfg;
        this.radius = cfg.radius();
        this.noiseThreshold = cfg.noiseThreshold().orElse(((float) cfg.radius() + 0.0001F) / 10.0F);
        this.decorators = cfg.decorators().stream().map(d -> new Decorator(d, world)).toList();
    }

    private ConfiguredShell() {
        this.cfg = ShellSettings.builder().build();
        this.radius = 0.0;
        this.noiseThreshold = 0.0F;
        this.decorators = List.of();
    }

    public static final class Decorator {

        public final ShellSettings.Decorator cfg;
        public final FastNoise noise;

        private Decorator(ShellSettings.Decorator cfg, World world) {
            this.cfg = cfg;
            this.noise = cfg.noise().map(n -> n.getGenerator(world)).orElse(new DummyGenerator(0F));
        }

        public boolean matches(IBlockState state) {
            if (cfg.matchers().isEmpty()) {
                return state.isOpaqueCube();
            }
            return cfg.matchers().contains(state);
        }

        /**
         * Returns true if the replacement doesn't have noise or
         * if its noise at the given coords meets the threshold.
         */
        public boolean testNoise(int x, int y, int z, int chunkX, int chunkZ) {
            int actualX = (chunkX * 16) + x;
            int actualZ = (chunkZ * 16) + z;
            return testNoise(actualX, y, actualZ);
        }

        /** Variant of testNoise() that uses absolute coordinates. */
        private boolean testNoise(int x, int y, int z) {
            return noise.GetBoolean(x, y, z);
        }

    }

}
