package com.personthecat.cleancaves.model;

import com.personthecat.cleancaves.data.ConditionSettings;
import com.personthecat.cleancaves.noise.DummyGenerator;
import fastnoise.FastNoise;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * {@link ConditionSettings} compiled against a specific world: biome and dimension tests are
 * reduced to predicates and the noise settings are materialized into generators, so the hot path
 * does no list scanning or option unwrapping.
 */
public final class Conditions {

    /** Any conditions for spawning this feature according to the current biome. */
    public final Predicate<Biome> biomes;

    /** Indicates whether this feature has specific biome restrictions. */
    public final boolean hasBiomes;

    /** Indicates whether this feature has noise-based region restrictions. */
    public final boolean hasRegion;

    /** Any conditions for spawning this feature according to the current dimension. */
    public final Predicate<Integer> dimensions;

    /** Height restrictions for the current feature. */
    public final Range height;

    /** The value produced by this generator will augment the maximum height level. */
    public final FastNoise floor;

    /** The value produced by this generator will augment the minimum height level. */
    public final FastNoise ceiling;

    /** 2-dimensional noise constraints for this feature. */
    public final FastNoise region;

    /** 3-dimensional noise constraints for this feature. */
    public final FastNoise noise;

    private Conditions(ConditionSettings settings, World world) {
        this.hasBiomes = settings.blacklistBiomes() || !settings.biomes().isEmpty();
        this.hasRegion = settings.region().isPresent();
        this.biomes = compileBiomes(settings);
        this.dimensions = compileDimensions(settings);
        this.height = settings.height();
        this.floor = compile(settings.floor().map(c -> c.getGenerator(world)));
        this.ceiling = compile(settings.ceiling().map(c -> c.getGenerator(world)));
        this.region = compile(settings.region().map(c -> c.getGenerator(world)));
        this.noise = compile(settings.noise().map(c -> c.getGenerator(world)));
    }

    public static Conditions compile(ConditionSettings settings, World world) {
        return new Conditions(settings, world);
    }

    private static FastNoise compile(java.util.Optional<FastNoise> generator) {
        return generator.orElseGet(() -> new DummyGenerator(0F));
    }

    private static Predicate<Biome> compileBiomes(ConditionSettings settings) {
        final List<Biome> list = settings.biomes();
        if (list.isEmpty()) {
            return b -> true;
        } else if (list.size() == 1) {
            final Biome listed = list.get(0);
            return settings.blacklistBiomes() ? b -> !listed.equals(b) : listed::equals;
        }
        final List<Biome> nonRedundant = List.copyOf(new ArrayList<>(new HashSet<>(list)));
        return settings.blacklistBiomes() ? b -> !nonRedundant.contains(b) : nonRedundant::contains;
    }

    private static Predicate<Integer> compileDimensions(ConditionSettings settings) {
        final List<Integer> list = settings.dimensions();
        if (list.isEmpty()) {
            return d -> true;
        } else if (list.size() == 1) {
            final int listed = list.get(0);
            return settings.blacklistDimensions() ? d -> listed != d : d -> listed == d;
        }
        final IntList nonRedundant = IntLists.unmodifiable(new IntArrayList(new HashSet<>(list)));
        return settings.blacklistDimensions() ? d -> !nonRedundant.contains(d) : nonRedundant::contains;
    }

    /** Get the current height range when given two absolute coordinates. */
    public Range getColumn(int x, int z) {
        final int min = this.height.min() + (int) this.floor.GetAdjustedNoise((float) x, (float) z);
        final int max = this.height.max() + (int) this.ceiling.GetAdjustedNoise((float) x, (float) z);
        return Range.checkedOrEmpty(min, max);
    }

    /** As above, but clamped to the surface recorded in the current chunk's heightmap. */
    public Range getColumn(int[][] heightmap, int x, int z) {
        final int min = this.height.min() + (int) this.floor.GetAdjustedNoise((float) x, (float) z);
        final int max = Math.min(this.height.max(), heightmap[x & 15][z & 15])
                + (int) this.ceiling.GetAdjustedNoise((float) x, (float) z);
        return Range.checkedOrEmpty(min, max);
    }

}
