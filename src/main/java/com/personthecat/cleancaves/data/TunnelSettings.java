package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.model.ScalableFloat;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import java.util.Optional;

import static com.personthecat.cleancaves.util.CommonMethods.invert;

/** Vanilla-style winding tunnels, with optional rooms at their origins and nested branches. */
public record TunnelSettings(
    ConditionSettings conditions,
    DecoratorSettings decorators,
    Range originHeight,
    boolean noiseYReduction,
    boolean resizeBranches,
    Optional<TunnelSettings> branches,
    Optional<RoomSettings> rooms,
    ScalableFloat dYaw,
    ScalableFloat dPitch,
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
    boolean checkWater
) {
    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Keys.tunnels;

    /** Default spawn conditions for all tunnel generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(8, 128)).build();

    /** Default decorator settings for all tunnel generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    /** JSON key names. */
    public static final class Keys {
        public static final String conditions = "conditions";
        public static final String decorators = "decorators";
        public static final String originHeight = "originHeight";
        public static final String noiseYReduction = "noiseYReduction";
        public static final String resizeBranches = "resizeBranches";
        public static final String branches = "branches";
        public static final String rooms = "rooms";
        public static final String dYaw = "dYaw";
        public static final String dPitch = "dPitch";
        public static final String scale = "scale";
        public static final String stretch = "stretch";
        public static final String yaw = "yaw";
        public static final String pitch = "pitch";
        public static final String systemChance = "systemChance";
        public static final String chance = "chance";
        public static final String systemDensity = "systemDensity";
        public static final String distance = "distance";
        public static final String count = "count";
        public static final String resolution = "resolution";
        public static final String seed = "seed";
        public static final String reseedBranches = "reseedBranches";
        public static final String hasBranches = "hasBranches";
        public static final String checkWater = "checkWater";

        private Keys() {}
    }

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
            .mapRangeOrTry(Keys.originHeight, ConditionSettings.Keys.height, builder::originHeight)
            .mapBool(Keys.noiseYReduction, builder::noiseYReduction)
            .mapBool(Keys.resizeBranches, builder::resizeBranches)
            .mapObject(Keys.branches, o -> builder.branches(Optional.of(from(o))))
            .mapObject(Keys.rooms, o -> builder.rooms(Optional.of(RoomSettings.from(o))))
            .mapScalableFloat(Keys.dYaw, original.dYaw, builder::dYaw)
            .mapScalableFloat(Keys.dPitch, original.dPitch, builder::dPitch)
            .mapScalableFloat(Keys.scale, original.scale, builder::scale)
            .mapScalableFloat(Keys.stretch, original.stretch, builder::stretch)
            .mapScalableFloat(Keys.yaw, original.yaw, builder::yaw)
            .mapScalableFloat(Keys.pitch, original.pitch, builder::pitch)
            .mapFloat(Keys.systemChance, f -> builder.systemChance(invert(f)))
            .mapFloat(Keys.chance, f -> builder.chance(invert(f)))
            .mapInt(Keys.systemDensity, builder::systemDensity)
            .mapInt(Keys.distance, builder::distance)
            .mapInt(Keys.count, builder::count)
            .mapInt(Keys.resolution, builder::resolution)
            .mapInt(Keys.seed, i -> builder.seed(Optional.of((long) i)))
            .mapBool(Keys.reseedBranches, builder::reseedBranches)
            .mapBool(Keys.hasBranches, builder::hasBranches)
            .mapBool(Keys.checkWater, builder::checkWater)
            .release(builder::build);
    }

    public static final class Builder {
        private ConditionSettings conditions = DEFAULT_CONDITIONS;
        private DecoratorSettings decorators = DEFAULT_DECORATORS;
        private Range originHeight = Range.of(8, 128);
        private boolean noiseYReduction = true;
        private boolean resizeBranches = true;
        private Optional<TunnelSettings> branches = Optional.empty();
        private Optional<RoomSettings> rooms = Optional.empty();
        private ScalableFloat dYaw = new ScalableFloat(0.0F, 0.0F, 0.75F, 4.0F, 1.0F);
        private ScalableFloat dPitch = new ScalableFloat(0.0f, 0.0f, 0.9f, 2.0F, 1.0F);
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

        private Builder() {}

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

        public Builder dYaw(ScalableFloat dYaw) {
            this.dYaw = dYaw;
            return this;
        }

        public Builder dPitch(ScalableFloat dPitch) {
            this.dPitch = dPitch;
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
            return new TunnelSettings(this.conditions, this.decorators, this.originHeight,
                this.noiseYReduction, this.resizeBranches, this.branches, this.rooms, this.dYaw,
                this.dPitch, this.scale, this.stretch, this.yaw, this.pitch, this.systemChance,
                this.chance, this.systemDensity, this.distance, this.count, this.resolution,
                this.seed, this.reseedBranches, this.hasBranches, this.checkWater);
        }
    }
}
