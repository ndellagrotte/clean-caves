package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.model.ScalableFloat;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import java.util.Optional;

import static com.personthecat.cleancaves.util.CommonMethods.invert;

/** Vanilla-style winding tunnels, with optional rooms at their origins and nested branches. */
public record TunnelSettings(ConditionSettings conditions,
        DecoratorSettings decorators,
        Range originHeight,
        boolean noiseYReduction,
        boolean resizeBranches,
        Optional<TunnelSettings> branches,
        Optional<RoomSettings> rooms,
        ScalableFloat deltaYaw,
        ScalableFloat deltaPitch,
        ScalableFloat scale,
        ScalableFloat stretch,
        ScalableFloat yaw,
        ScalableFloat pitch,
        int systemChance,
        int chance,
        int systemDensity,
        int distance,
        int count,
        int resolution,
        Optional<Long> seed,
        boolean reseedBranches,
        boolean hasBranches,
        boolean checkWater) {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Keys.TUNNELS;

    /** Default spawn conditions for all tunnel generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
            .height(Range.of(8, 128))
            .build();

    /** Default decorator settings for all tunnel generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    public static Builder builder() {
        return new Builder();
    }

    public static TunnelSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        final Builder builder = overrides.apply(builder());
        return copyInto(json, builder.conditions(conditions).decorators(decorators));
    }

    public static TunnelSettings from(JsonObject json, ConditionSettings conditions, DecoratorSettings decorators) {
        return copyInto(json, builder().conditions(conditions).decorators(decorators));
    }

    public static TunnelSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static TunnelSettings copyInto(JsonObject json, Builder builder) {
        // Captured up front: the scalable floats below inherit from whatever the builder started
        // with, not from any value a preceding mapper wrote.
        final TunnelSettings original = builder.build();
        return new HjsonMapper(json)
                .mapSelf(o -> builder.conditions(ConditionSettings.from(o, original.conditions)))
                .mapSelf(o -> builder.decorators(DecoratorSettings.from(o, original.decorators)))
                .mapRangeOrTry(Keys.ORIGIN_HEIGHT, ConditionSettings.Keys.HEIGHT, builder::originHeight)
                .mapBool(Keys.NOISE_YREDUCTION, builder::noiseYReduction)
                .mapBool(Keys.RESIZE_BRANCHES, builder::resizeBranches)
                .mapObject(Keys.BRANCHES, o -> builder.branches(Optional.of(from(o))))
                .mapObject(Keys.ROOMS, o -> builder.rooms(Optional.of(RoomSettings.from(o))))
                .mapScalableFloat(Keys.D_YAW, original.deltaYaw, builder::deltaYaw)
                .mapScalableFloat(Keys.D_PITCH, original.deltaPitch, builder::deltaPitch)
                .mapScalableFloat(Keys.SCALE, original.scale, builder::scale)
                .mapScalableFloat(Keys.STRETCH, original.stretch, builder::stretch)
                .mapScalableFloat(Keys.YAW, original.yaw, builder::yaw)
                .mapScalableFloat(Keys.PITCH, original.pitch, builder::pitch)
                .mapFloat(Keys.SYSTEM_CHANCE, f -> builder.systemChance(invert(f)))
                .mapFloat(Keys.CHANCE, f -> builder.chance(invert(f)))
                .mapInt(Keys.SYSTEM_DENSITY, builder::systemDensity)
                .mapInt(Keys.DISTANCE, builder::distance)
                .mapInt(Keys.COUNT, builder::count)
                .mapInt(Keys.RESOLUTION, builder::resolution)
                .mapInt(Keys.SEED, i -> builder.seed(Optional.of((long) i)))
                .mapBool(Keys.RESEED_BRANCHES, builder::reseedBranches)
                .mapBool(Keys.HAS_BRANCHES, builder::hasBranches)
                .mapBool(Keys.CHECK_WATER, builder::checkWater)
                .release(builder::build);
    }

    /** JSON key names. */
    public static final class Keys {

        public static final String CONDITIONS = "conditions";
        public static final String DECORATORS = "decorators";
        public static final String ORIGIN_HEIGHT = "originHeight";
        public static final String NOISE_YREDUCTION = "noiseYReduction";
        public static final String RESIZE_BRANCHES = "resizeBranches";
        public static final String BRANCHES = "branches";
        public static final String ROOMS = "rooms";
        public static final String D_YAW = "dYaw";
        public static final String D_PITCH = "dPitch";
        public static final String SCALE = "scale";
        public static final String STRETCH = "stretch";
        public static final String YAW = "yaw";
        public static final String PITCH = "pitch";
        public static final String SYSTEM_CHANCE = "systemChance";
        public static final String CHANCE = "chance";
        public static final String SYSTEM_DENSITY = "systemDensity";
        public static final String DISTANCE = "distance";
        public static final String COUNT = "count";
        public static final String RESOLUTION = "resolution";
        public static final String SEED = "seed";
        public static final String RESEED_BRANCHES = "reseedBranches";
        public static final String HAS_BRANCHES = "hasBranches";
        public static final String CHECK_WATER = "checkWater";

        private Keys() { }

    }

    public static final class Builder {

        private ConditionSettings conditions = DEFAULT_CONDITIONS;
        private DecoratorSettings decorators = DEFAULT_DECORATORS;
        private Range originHeight = Range.of(8, 128);
        private boolean noiseYReduction = true;
        private boolean resizeBranches = true;
        private Optional<TunnelSettings> branches = Optional.empty();
        private Optional<RoomSettings> rooms = Optional.empty();
        private ScalableFloat deltaYaw = new ScalableFloat(0.0F, 0.0F, 0.75F, 4.0F, 1.0F);
        private ScalableFloat deltaPitch = new ScalableFloat(0.0f, 0.0f, 0.9f, 2.0F, 1.0F);
        private ScalableFloat scale = new ScalableFloat(0.0F, 1.0F, 1.0F, 0.0F, 1.0F);
        private ScalableFloat stretch = new ScalableFloat(1.0F, 1.0F, 1.0F, 0.0F, 1.0F);
        private ScalableFloat yaw = new ScalableFloat(0.0F, 1.0F, 1.0F, 0.0F, 1.0F);
        private ScalableFloat pitch = new ScalableFloat(0.0F, 0.25F, 1.0F, 0.0F, 1.0F);
        private int systemChance = 4;
        private int chance = 7;
        private int systemDensity = 4;
        private int distance = 0;
        private int count = 15;
        private int resolution = 4;
        private Optional<Long> seed = Optional.empty();
        private boolean reseedBranches = true;
        private boolean hasBranches = true;
        private boolean checkWater = true;

        private Builder() { }

        public Builder conditions(ConditionSettings conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder decorators(DecoratorSettings decorators) {
            this.decorators = decorators;
            return this;
        }

        public Builder originHeight(Range originHeight) {
            this.originHeight = originHeight;
            return this;
        }

        public Builder noiseYReduction(boolean noiseYReduction) {
            this.noiseYReduction = noiseYReduction;
            return this;
        }

        public Builder resizeBranches(boolean resizeBranches) {
            this.resizeBranches = resizeBranches;
            return this;
        }

        public Builder branches(Optional<TunnelSettings> branches) {
            this.branches = branches;
            return this;
        }

        public Builder rooms(Optional<RoomSettings> rooms) {
            this.rooms = rooms;
            return this;
        }

        public Builder deltaYaw(ScalableFloat deltaYaw) {
            this.deltaYaw = deltaYaw;
            return this;
        }

        public Builder deltaPitch(ScalableFloat deltaPitch) {
            this.deltaPitch = deltaPitch;
            return this;
        }

        public Builder scale(ScalableFloat scale) {
            this.scale = scale;
            return this;
        }

        public Builder stretch(ScalableFloat stretch) {
            this.stretch = stretch;
            return this;
        }

        public Builder yaw(ScalableFloat yaw) {
            this.yaw = yaw;
            return this;
        }

        public Builder pitch(ScalableFloat pitch) {
            this.pitch = pitch;
            return this;
        }

        public Builder systemChance(int systemChance) {
            this.systemChance = systemChance;
            return this;
        }

        public Builder chance(int chance) {
            this.chance = chance;
            return this;
        }

        public Builder systemDensity(int systemDensity) {
            this.systemDensity = systemDensity;
            return this;
        }

        public Builder distance(int distance) {
            this.distance = distance;
            return this;
        }

        public Builder count(int count) {
            this.count = count;
            return this;
        }

        public Builder resolution(int resolution) {
            this.resolution = resolution;
            return this;
        }

        public Builder seed(Optional<Long> seed) {
            this.seed = seed;
            return this;
        }

        public Builder reseedBranches(boolean reseedBranches) {
            this.reseedBranches = reseedBranches;
            return this;
        }

        public Builder hasBranches(boolean hasBranches) {
            this.hasBranches = hasBranches;
            return this;
        }

        public Builder checkWater(boolean checkWater) {
            this.checkWater = checkWater;
            return this;
        }

        public TunnelSettings build() {
            return new TunnelSettings(this.conditions, this.decorators, this.originHeight, this.noiseYReduction,
                    this.resizeBranches, this.branches, this.rooms, this.deltaYaw, this.deltaPitch, this.scale,
                    this.stretch, this.yaw, this.pitch, this.systemChance, this.chance, this.systemDensity,
                    this.distance, this.count, this.resolution, this.seed, this.reseedBranches, this.hasBranches,
                    this.checkWater);
        }

    }

}
