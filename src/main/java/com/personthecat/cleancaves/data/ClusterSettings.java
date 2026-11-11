package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.hjson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import java.util.List;
import java.util.Objects;

/** Data used for spawning giant clusters of stone through ChunkPrimer. */
public record ClusterSettings(ConditionSettings conditions,
        List<Pair<IBlockState, Integer>> states,
        double selectionThreshold,
        double chance,
        double integrity,
        Range radiusX,
        Range radiusY,
        Range radiusZ,
        Range centerHeight,
        List<IBlockState> matchers,
        boolean spawnInAir) {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Keys.CLUSTERS;

    /** Default spawn conditions for all cluster generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder().build();

    /** Default values for cluster noise. */
    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
            .frequency(0.0143f)
            .threshold(Range.of(-0.6F))
            .stretch(0.5f)
            .octaves(1)
            .build();

    /** JSON key names. */
    public static final class Keys {

        public static final String CONDITIONS = "conditions";
        public static final String STATES = "states";
        public static final String SELECTION_THRESHOLD = "selectionThreshold";
        public static final String CHANCE = "chance";
        public static final String INTEGRITY = "integrity";
        /** Shorthand which sets all three axes at once. */
        public static final String RADIUS = "radius";
        public static final String RADIUS_X = "radiusX";
        public static final String RADIUS_Y = "radiusY";
        public static final String RADIUS_Z = "radiusZ";
        public static final String CENTER_HEIGHT = "centerHeight";
        public static final String MATCHERS = "matchers";
        public static final String SPAWN_IN_AIR = "spawnInAir";
        public static final String SEED = "seed";

        private Keys() { }

    }

    public static Builder builder() {
        return new Builder();
    }

    public static ClusterSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static ClusterSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static ClusterSettings copyInto(JsonObject json, Builder builder) {
        final ConditionSettings inherited = builder.conditions;
        return new HjsonMapper(json)
                .mapRequiredStateList(Keys.STATES, FEATURE_NAME, l -> copyStates(builder, l))
                .mapSelf(o -> builder.conditions(ConditionSettings.from(o, inherited)))
                .mapFloat(Keys.CHANCE, f -> copyChance(builder, f))
                .mapFloat(Keys.INTEGRITY, builder::integrity)
                .mapRange(Keys.RADIUS, r -> copyRadius(builder, r))
                .mapRange(Keys.RADIUS_X, builder::radiusX)
                .mapRange(Keys.RADIUS_Y, builder::radiusY)
                .mapRange(Keys.RADIUS_Z, builder::radiusZ)
                .mapRange(Keys.CENTER_HEIGHT, builder::centerHeight)
                .mapStateList(Keys.MATCHERS, builder::matchers)
                .mapBool(Keys.SPAWN_IN_AIR, builder::spawnInAir)
                .mapInt(Keys.SEED, i -> copySeed(builder, i))
                .release(builder::build);
    }

    private static void copyStates(Builder builder, List<IBlockState> states) {
        builder.states(states.stream().map(s -> Pair.of(s, Block.getStateId(s))).toList());
    }

    private static void copyChance(Builder builder, float chance) {
        builder.chance(chance).selectionThreshold((1.0F - chance) * 92.0F);
    }

    private static void copyRadius(Builder builder, Range radius) {
        builder.radiusX(radius).radiusY(radius).radiusZ(radius);
    }

    private static void copySeed(Builder builder, int seed) {
        // States must already be set at this point. The order matters here.
        final List<Pair<IBlockState, Integer>> states = Objects.requireNonNull(builder.states, "Out of order");
        builder.states(states.stream().map(p -> Pair.of(p.getLeft(), seed)).toList());
    }

    /** Returns whether this cluster is valid at these coordinates. */
    public boolean canSpawn(IBlockState state) {
        if (state.getBlock().equals(Blocks.AIR)) {
            return this.spawnInAir;
        }
        if (this.matchers.isEmpty()) {
            // By default, only replace stone blocks.
            return !Blocks.BEDROCK.equals(state.getBlock()) && Material.ROCK.equals(state.getMaterial());
        }
        return this.matchers.contains(state);
    }

    public static final class Builder {

        ConditionSettings conditions = DEFAULT_CONDITIONS;
        /** Required, and left null so that {@code copySeed} can assert the mapping order. */
        List<Pair<IBlockState, Integer>> states;
        private double selectionThreshold = 78.2;
        private double chance = 0.15;
        private double integrity = 1.0;
        private Range radiusX = Range.of(13, 19);
        private Range radiusY = Range.of(9, 15);
        private Range radiusZ = Range.of(13, 19);
        private Range centerHeight = Range.of(24, 40);
        private List<IBlockState> matchers = List.of();
        private boolean spawnInAir = false;

        private Builder() { }

        public Builder conditions(ConditionSettings conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder states(List<Pair<IBlockState, Integer>> states) {
            this.states = states;
            return this;
        }

        public Builder selectionThreshold(double selectionThreshold) {
            this.selectionThreshold = selectionThreshold;
            return this;
        }

        public Builder chance(double chance) {
            this.chance = chance;
            return this;
        }

        public Builder integrity(double integrity) {
            this.integrity = integrity;
            return this;
        }

        public Builder radiusX(Range radiusX) {
            this.radiusX = radiusX;
            return this;
        }

        public Builder radiusY(Range radiusY) {
            this.radiusY = radiusY;
            return this;
        }

        public Builder radiusZ(Range radiusZ) {
            this.radiusZ = radiusZ;
            return this;
        }

        public Builder centerHeight(Range centerHeight) {
            this.centerHeight = centerHeight;
            return this;
        }

        public Builder matchers(List<IBlockState> matchers) {
            this.matchers = matchers;
            return this;
        }

        public Builder spawnInAir(boolean spawnInAir) {
            this.spawnInAir = spawnInAir;
            return this;
        }

        public ClusterSettings build() {
            return new ClusterSettings(this.conditions, this.states, this.selectionThreshold, this.chance,
                    this.integrity, this.radiusX, this.radiusY, this.radiusZ, this.centerHeight, this.matchers,
                    this.spawnInAir);
        }

    }

}
