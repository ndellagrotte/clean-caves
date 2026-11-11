package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import java.util.Optional;

/** Noise-map pairs which trace a "worm" through the world, producing organic tunnels. */
public record BurrowSettings(ConditionSettings conditions,
        DecoratorSettings decorators,
        NoiseMapSettings map,
        NoiseMapSettings offset,
        float radius,
        float target,
        float stretch,
        float exponent,
        float shift,
        float wallDistance,
        float wallExponent,
        Optional<TunnelSettings> branches) {

    /** The default noise settings used for the map generator. */
    private static final NoiseMapSettings DEFAULT_MAP = NoiseMapSettings.builder()
            .frequency(0.005F)
            .perturb(true)
            .perturbAmp(0.1F)
            .perturbFreq(2.5F)
            .build();

    /** The default noise settings used for the offset generator. */
    private static final NoiseMapSettings DEFAULT_OFFSET = NoiseMapSettings.builder()
            .frequency(0.01F)
            .range(Range.of(10, 30))
            .build();

    /** Default spawn conditions for all burrow generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
            .height(Range.of(10, 50))
            .build();

    /** Default decorator settings for all burrow generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    public static Builder builder() {
        return new Builder();
    }

    public static BurrowSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return from(json, builder().conditions(conditions).decorators(decorators));
    }

    public static BurrowSettings from(JsonObject json, Builder builder) {
        final ConditionSettings inheritedConditions = builder.conditions;
        final DecoratorSettings inheritedDecorators = builder.decorators;
        return new HjsonMapper(json)
                .mapSelf(o -> builder.conditions(ConditionSettings.from(o, inheritedConditions)))
                .mapSelf(o -> builder.decorators(DecoratorSettings.from(o, inheritedDecorators)))
                .mapObject(Keys.MAP, o -> builder.map(NoiseMapSettings.from(o, DEFAULT_MAP)))
                .mapObject(Keys.OFFSET, o -> builder.offset(NoiseMapSettings.from(o, DEFAULT_OFFSET)))
                .mapFloat(Keys.RADIUS, builder::radius)
                .mapFloat(Keys.TARGET, builder::target)
                .mapFloat(Keys.STRETCH, builder::stretch)
                .mapFloat(Keys.EXPONENT, builder::exponent)
                .mapFloat(Keys.SHIFT, builder::shift)
                .mapFloat(Keys.WALL_DISTANCE, builder::wallDistance)
                .mapFloat(Keys.WALL_EXPONENT, builder::wallExponent)
                // Branches inherit the conditions and decorators resolved for this burrow.
                .mapObject(Keys.BRANCHES,
                        o -> builder.branches(Optional.of(TunnelSettings.from(o, builder.conditions,
                                builder.decorators))))
                .release(builder::build);
    }

    /** JSON key names. */
    public static final class Keys {

        public static final String CONDITIONS = "conditions";
        public static final String DECORATORS = "decorators";
        public static final String MAP = "map";
        public static final String OFFSET = "offset";
        public static final String RADIUS = "radius";
        public static final String TARGET = "target";
        public static final String STRETCH = "stretch";
        public static final String EXPONENT = "exponent";
        public static final String SHIFT = "shift";
        public static final String WALL_DISTANCE = "wallDistance";
        public static final String WALL_EXPONENT = "wallExponent";
        public static final String BRANCHES = "branches";

        private Keys() { }

    }

    public static final class Builder {

        ConditionSettings conditions = DEFAULT_CONDITIONS;
        DecoratorSettings decorators = DEFAULT_DECORATORS;
        private NoiseMapSettings map = DEFAULT_MAP;
        private NoiseMapSettings offset = DEFAULT_OFFSET;
        private float radius = 4.5F;
        private float target = 0.1F;
        private float stretch = 1.0F;
        private float exponent = 4.0F;
        private float shift = 0.0F;
        private float wallDistance = 18.0F;
        private float wallExponent = 2.0F;
        private Optional<TunnelSettings> branches = Optional.empty();

        private Builder() { }

        public Builder conditions(ConditionSettings conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder decorators(DecoratorSettings decorators) {
            this.decorators = decorators;
            return this;
        }

        public Builder map(NoiseMapSettings map) {
            this.map = map;
            return this;
        }

        public Builder offset(NoiseMapSettings offset) {
            this.offset = offset;
            return this;
        }

        public Builder radius(float radius) {
            this.radius = radius;
            return this;
        }

        public Builder target(float target) {
            this.target = target;
            return this;
        }

        public Builder stretch(float stretch) {
            this.stretch = stretch;
            return this;
        }

        public Builder exponent(float exponent) {
            this.exponent = exponent;
            return this;
        }

        public Builder shift(float shift) {
            this.shift = shift;
            return this;
        }

        public Builder wallDistance(float wallDistance) {
            this.wallDistance = wallDistance;
            return this;
        }

        public Builder wallExponent(float wallExponent) {
            this.wallExponent = wallExponent;
            return this;
        }

        public Builder branches(Optional<TunnelSettings> branches) {
            this.branches = branches;
            return this;
        }

        public BurrowSettings build() {
            return new BurrowSettings(this.conditions, this.decorators, this.map, this.offset, this.radius, this.target,
                    this.stretch, this.exponent, this.shift, this.wallDistance, this.wallExponent, this.branches);
        }

    }

}
