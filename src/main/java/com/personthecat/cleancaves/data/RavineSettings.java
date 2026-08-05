package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.model.ScalableFloat;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import static com.personthecat.cleancaves.util.CommonMethods.invert;

/** Vanilla-style ravines, optionally with noise-driven wall irregularity. */
public record RavineSettings(
    ConditionSettings conditions,
    DecoratorSettings decorators,
    Range originHeight,
    float noiseYFactor,
    ScalableFloat dYaw,
    ScalableFloat dPitch,
    ScalableFloat scale,
    ScalableFloat stretch,
    ScalableFloat yaw,
    ScalableFloat pitch,
    int distance,
    int chance,
    int resolution,
    double cutoffStrength,
    boolean useWallNoise,
    boolean checkWater,
    NoiseMapSettings walls
) {
    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Keys.ravines;

    /** Default spawn conditions for all ravine generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(20, 66)).build();

    /** Default decorator settings for all ravine generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    /** JSON key names. */
    public static final class Keys {
        public static final String conditions = "conditions";
        public static final String decorators = "decorators";
        public static final String originHeight = "originHeight";
        public static final String noiseYFactor = "noiseYFactor";
        public static final String dYaw = "dYaw";
        public static final String dPitch = "dPitch";
        public static final String scale = "scale";
        public static final String stretch = "stretch";
        public static final String yaw = "yaw";
        public static final String pitch = "pitch";
        public static final String distance = "distance";
        public static final String chance = "chance";
        public static final String resolution = "resolution";
        public static final String cutoffStrength = "cutoffStrength";
        public static final String useWallNoise = "useWallNoise";
        public static final String checkWater = "checkWater";
        public static final String walls = "walls";

        private Keys() {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RavineSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions).decorators(decorators));
    }

    public static RavineSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static RavineSettings copyInto(JsonObject json, Builder builder) {
        final RavineSettings original = builder.build();
        return new HjsonMapper(json)
            .mapSelf(o -> builder.conditions(ConditionSettings.from(o, original.conditions)))
            .mapSelf(o -> builder.decorators(DecoratorSettings.from(o, original.decorators)))
            .mapRangeOrTry(Keys.originHeight, ConditionSettings.Keys.height, builder::originHeight)
            .mapFloat(Keys.noiseYFactor, builder::noiseYFactor)
            .mapScalableFloat(Keys.dYaw, original.dYaw, builder::dYaw)
            .mapScalableFloat(Keys.dPitch, original.dPitch, builder::dPitch)
            .mapScalableFloat(Keys.scale, original.scale, builder::scale)
            .mapScalableFloat(Keys.stretch, original.stretch, builder::stretch)
            .mapScalableFloat(Keys.yaw, original.yaw, builder::yaw)
            .mapScalableFloat(Keys.pitch, original.pitch, builder::pitch)
            .mapInt(Keys.distance, builder::distance)
            .mapFloat(Keys.chance, f -> builder.chance(invert(f)))
            .mapInt(Keys.resolution, builder::resolution)
            .mapFloat(Keys.cutoffStrength, builder::cutoffStrength)
            // Declaring a walls object is itself what turns wall noise on.
            .mapObject(Keys.walls, o ->
                builder.useWallNoise(true).walls(NoiseMapSettings.from(o, original.walls)))
            .mapBool(Keys.useWallNoise, builder::useWallNoise)
            .mapBool(Keys.checkWater, builder::checkWater)
            .release(builder::build);
    }

    public static final class Builder {
        private ConditionSettings conditions = DEFAULT_CONDITIONS;
        private DecoratorSettings decorators = DEFAULT_DECORATORS;
        private Range originHeight = Range.of(20, 66);
        private float noiseYFactor = 0.7F;
        private ScalableFloat dYaw = new ScalableFloat(0.0F, 0.0F, 0.5F, 4.0F, 1.0F);
        private ScalableFloat dPitch = new ScalableFloat(0.0F, 0.0F, 0.8F, 2.0F, 1.0F);
        private ScalableFloat scale = new ScalableFloat(0.0F, 2.0F, 1.0F, 0.0F, 1.0F);
        private ScalableFloat stretch = new ScalableFloat(3.0F, 1.0F, 1.0F, 0.0F, 1.0F);
        private ScalableFloat yaw = new ScalableFloat(0.0F, 1.0F, 1.0F, 0.0F, 1.0F);
        private ScalableFloat pitch = new ScalableFloat(0.0F, 0.25F, 1.0F, 0.0F, 1.0F);
        private int distance = 0;
        private int chance = 5;
        private int resolution = 4;
        private double cutoffStrength = 5.0;
        private boolean useWallNoise = false;
        private boolean checkWater = true;
        private NoiseMapSettings walls = NoiseMapSettings.builder()
            .frequency(0.5F).range(Range.of(0, 4)).build();

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

        public Builder noiseYFactor(float noiseYFactor) {
            this.noiseYFactor = noiseYFactor;
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

        public Builder distance(int distance) {
            this.distance = distance;
            return this;
        }

        public Builder chance(int chance) {
            this.chance = chance;
            return this;
        }

        public Builder resolution(int resolution) {
            this.resolution = resolution;
            return this;
        }

        public Builder cutoffStrength(double cutoffStrength) {
            this.cutoffStrength = cutoffStrength;
            return this;
        }

        public Builder useWallNoise(boolean useWallNoise) {
            this.useWallNoise = useWallNoise;
            return this;
        }

        public Builder checkWater(boolean checkWater) {
            this.checkWater = checkWater;
            return this;
        }

        public Builder walls(NoiseMapSettings walls) {
            this.walls = walls;
            return this;
        }

        public RavineSettings build() {
            return new RavineSettings(this.conditions, this.decorators, this.originHeight,
                this.noiseYFactor, this.dYaw, this.dPitch, this.scale, this.stretch, this.yaw,
                this.pitch, this.distance, this.chance, this.resolution, this.cutoffStrength,
                this.useWallNoise, this.checkWater, this.walls);
        }
    }
}
