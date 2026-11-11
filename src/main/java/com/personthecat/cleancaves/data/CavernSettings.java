package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import java.util.List;
import java.util.Optional;

/** Large open spaces carved by one or more 3-dimensional noise generators. */
public record CavernSettings(ConditionSettings conditions,
        DecoratorSettings decorators,
        int resolution,
        Optional<NoiseMapSettings> offset,
        Optional<NoiseMapSettings> walls,
        Optional<NoiseMapSettings> wallOffset,
        float wallCurveRatio,
        boolean wallInterpolation,
        List<NoiseSettings> generators,
        Optional<TunnelSettings> branches,
        boolean deferred) {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Keys.CAVERNS;

    /** The default noise generator settings used by the caverns feature. */
    private static final NoiseSettings DEFAULT_GENERATOR = NoiseSettings.builder()
            .frequency(0.0143F)
            .threshold(Range.of(-0.6F))
            .stretch(0.5F)
            .octaves(1)
            .build();

    /** The default ceiling noise parameters used by caverns, if absent. */
    private static final NoiseMapSettings DEFAULT_CEIL_NOISE = NoiseMapSettings.builder()
            .frequency(0.02F)
            .range(Range.of(-17, -3))
            .build();

    /** The default floor noise parameters used by caverns, if absent. */
    private static final NoiseMapSettings DEFAULT_FLOOR_NOISE = NoiseMapSettings.builder()
            .frequency(0.02F)
            .range(Range.of(0, 8))
            .build();

    private static final NoiseMapSettings DEFAULT_HEIGHT_OFFSET = NoiseMapSettings.builder()
            .frequency(0.005F)
            .range(Range.of(0, 50))
            .build();

    /** The default wall noise used at biome borders. */
    private static final NoiseMapSettings DEFAULT_WALL_NOISE = NoiseMapSettings.builder()
            .frequency(0.02F)
            .range(Range.of(9, 15))
            .build();

    /** Transformations for biome walls for this generator. */
    private static final NoiseMapSettings DEFAULT_WALL_OFFSET = NoiseMapSettings.builder()
            .frequency(0.05F)
            .range(Range.of(0, 255))
            .build();

    /** Default spawn conditions for all cavern generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
            .height(Range.of(10, 50))
            .ceiling(Optional.of(DEFAULT_CEIL_NOISE))
            .floor(Optional.of(DEFAULT_FLOOR_NOISE))
            .build();

    /** Default decorator settings for all cavern generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    public static Builder builder() {
        return new Builder();
    }

    public static CavernSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions).decorators(decorators));
    }

    public static CavernSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static CavernSettings copyInto(JsonObject json, Builder builder) {
        final ConditionSettings inheritedConditions = builder.conditions;
        final DecoratorSettings inheritedDecorators = builder.decorators;
        return new HjsonMapper(json)
                .mapSelf(o -> builder.conditions(ConditionSettings.from(o, inheritedConditions)))
                .mapSelf(o -> builder.decorators(DecoratorSettings.from(o, inheritedDecorators)))
                .mapInt(Keys.RESOLUTION, builder::resolution)
                .mapObject(Keys.OFFSET,
                        o -> builder.offset(Optional.of(NoiseMapSettings.from(o, DEFAULT_HEIGHT_OFFSET))))
                .mapObject(Keys.WALLS, o -> builder.walls(Optional.of(NoiseMapSettings.from(o, DEFAULT_WALL_NOISE))))
                .mapObject(Keys.WALL_OFFSET,
                        o -> builder.wallOffset(Optional.of(NoiseMapSettings.from(o, DEFAULT_WALL_OFFSET))))
                .mapFloat(Keys.WALL_CURVE_RATIO, builder::wallCurveRatio)
                .mapBool(Keys.WALL_INTERPOLATION, builder::wallInterpolation)
                .mapArray(Keys.GENERATORS, o -> NoiseSettings.from(o, DEFAULT_GENERATOR), builder::generators)
                // Branches inherit the conditions and decorators resolved for these caverns.
                .mapObject(Keys.BRANCHES,
                        o -> builder.branches(Optional.of(TunnelSettings.from(o, builder.conditions,
                                builder.decorators))))
                .mapBool(Keys.DEFERRED, builder::deferred)
                .release(builder::build);
    }

    /** JSON key names. */
    public static final class Keys {

        public static final String CONDITIONS = "conditions";
        public static final String DECORATORS = "decorators";
        public static final String RESOLUTION = "resolution";
        public static final String OFFSET = "offset";
        public static final String WALLS = "walls";
        public static final String WALL_OFFSET = "wallOffset";
        public static final String WALL_CURVE_RATIO = "wallCurveRatio";
        public static final String WALL_INTERPOLATION = "wallInterpolation";
        public static final String GENERATORS = "generators";
        public static final String BRANCHES = "branches";
        public static final String DEFERRED = "deferred";

        private Keys() { }

    }

    public static final class Builder {

        ConditionSettings conditions = DEFAULT_CONDITIONS;
        DecoratorSettings decorators = DEFAULT_DECORATORS;
        private int resolution = 1;
        private Optional<NoiseMapSettings> offset = Optional.empty();
        private Optional<NoiseMapSettings> walls = Optional.empty();
        private Optional<NoiseMapSettings> wallOffset = Optional.empty();
        private float wallCurveRatio = 1.0f;
        private boolean wallInterpolation = false;
        private List<NoiseSettings> generators = List.of(DEFAULT_GENERATOR);
        private Optional<TunnelSettings> branches = Optional.empty();
        private boolean deferred = false;

        private Builder() { }

        public Builder conditions(ConditionSettings conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder decorators(DecoratorSettings decorators) {
            this.decorators = decorators;
            return this;
        }

        public Builder resolution(int resolution) {
            this.resolution = resolution;
            return this;
        }

        public Builder offset(Optional<NoiseMapSettings> offset) {
            this.offset = offset;
            return this;
        }

        public Builder walls(Optional<NoiseMapSettings> walls) {
            this.walls = walls;
            return this;
        }

        public Builder wallOffset(Optional<NoiseMapSettings> wallOffset) {
            this.wallOffset = wallOffset;
            return this;
        }

        public Builder wallCurveRatio(float wallCurveRatio) {
            this.wallCurveRatio = wallCurveRatio;
            return this;
        }

        public Builder wallInterpolation(boolean wallInterpolation) {
            this.wallInterpolation = wallInterpolation;
            return this;
        }

        public Builder generators(List<NoiseSettings> generators) {
            this.generators = generators;
            return this;
        }

        public Builder branches(Optional<TunnelSettings> branches) {
            this.branches = branches;
            return this;
        }

        public Builder deferred(boolean deferred) {
            this.deferred = deferred;
            return this;
        }

        public CavernSettings build() {
            return new CavernSettings(this.conditions, this.decorators, this.resolution, this.offset, this.walls,
                    this.wallOffset, this.wallCurveRatio, this.wallInterpolation, this.generators, this.branches,
                    this.deferred);
        }

    }

}
