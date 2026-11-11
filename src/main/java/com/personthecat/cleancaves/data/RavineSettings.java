package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.model.ScalableFloat;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import static com.personthecat.cleancaves.util.CommonMethods.invert;

/** Vanilla-style ravines, optionally with noise-driven wall irregularity. */
public record RavineSettings(ConditionSettings conditions,
        DecoratorSettings decorators,
        Range originHeight,
        float noiseYFactor,
        ScalableFloat deltaYaw,
        ScalableFloat deltaPitch,
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
        NoiseMapSettings walls) {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Keys.RAVINES;

    /** Default spawn conditions for all ravine generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
            .height(Range.of(20, 66))
            .build();

    /** Default decorator settings for all ravine generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

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
                .mapRangeOrTry(Keys.ORIGIN_HEIGHT, ConditionSettings.Keys.HEIGHT, builder::originHeight)
                .mapFloat(Keys.NOISE_YFACTOR, builder::noiseYFactor)
                .mapScalableFloat(Keys.D_YAW, original.deltaYaw, builder::deltaYaw)
                .mapScalableFloat(Keys.D_PITCH, original.deltaPitch, builder::deltaPitch)
                .mapScalableFloat(Keys.SCALE, original.scale, builder::scale)
                .mapScalableFloat(Keys.STRETCH, original.stretch, builder::stretch)
                .mapScalableFloat(Keys.YAW, original.yaw, builder::yaw)
                .mapScalableFloat(Keys.PITCH, original.pitch, builder::pitch)
                .mapInt(Keys.DISTANCE, builder::distance)
                .mapFloat(Keys.CHANCE, f -> builder.chance(invert(f)))
                .mapInt(Keys.RESOLUTION, builder::resolution)
                .mapFloat(Keys.CUTOFF_STRENGTH, builder::cutoffStrength)
                // Declaring a walls object is itself what turns wall noise on.
                .mapObject(Keys.WALLS, o -> builder.useWallNoise(true).walls(NoiseMapSettings.from(o, original.walls)))
                .mapBool(Keys.USE_WALL_NOISE, builder::useWallNoise)
                .mapBool(Keys.CHECK_WATER, builder::checkWater)
                .release(builder::build);
    }

    /** JSON key names. */
    public static final class Keys {

        public static final String CONDITIONS = "conditions";
        public static final String DECORATORS = "decorators";
        public static final String ORIGIN_HEIGHT = "originHeight";
        public static final String NOISE_YFACTOR = "noiseYFactor";
        public static final String D_YAW = "dYaw";
        public static final String D_PITCH = "dPitch";
        public static final String SCALE = "scale";
        public static final String STRETCH = "stretch";
        public static final String YAW = "yaw";
        public static final String PITCH = "pitch";
        public static final String DISTANCE = "distance";
        public static final String CHANCE = "chance";
        public static final String RESOLUTION = "resolution";
        public static final String CUTOFF_STRENGTH = "cutoffStrength";
        public static final String USE_WALL_NOISE = "useWallNoise";
        public static final String CHECK_WATER = "checkWater";
        public static final String WALLS = "walls";

        private Keys() { }

    }

    public static final class Builder {

        private ConditionSettings conditions = DEFAULT_CONDITIONS;
        private DecoratorSettings decorators = DEFAULT_DECORATORS;
        private Range originHeight = Range.of(20, 66);
        private float noiseYFactor = 0.7F;
        private ScalableFloat deltaYaw = new ScalableFloat(0.0F, 0.0F, 0.5F, 4.0F, 1.0F);
        private ScalableFloat deltaPitch = new ScalableFloat(0.0F, 0.0F, 0.8F, 2.0F, 1.0F);
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
        private NoiseMapSettings walls = NoiseMapSettings.builder().frequency(0.5F).range(Range.of(0, 4)).build();

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

        public Builder noiseYFactor(float noiseYFactor) {
            this.noiseYFactor = noiseYFactor;
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
            return new RavineSettings(this.conditions, this.decorators, this.originHeight, this.noiseYFactor,
                    this.deltaYaw, this.deltaPitch, this.scale, this.stretch, this.yaw, this.pitch, this.distance,
                    this.chance, this.resolution, this.cutoffStrength, this.useWallNoise, this.checkWater, this.walls);
        }

    }

}
