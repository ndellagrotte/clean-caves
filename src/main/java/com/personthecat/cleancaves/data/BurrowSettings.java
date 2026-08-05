package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import java.util.Optional;

/** Noise-map pairs which trace a "worm" through the world, producing organic tunnels. */
public record BurrowSettings(
    ConditionSettings conditions,
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
    Optional<TunnelSettings> branches
) {
    /** The default noise settings used for the map generator. */
    private static final NoiseMapSettings DEFAULT_MAP = NoiseMapSettings.builder()
        .frequency(0.005F).perturb(true).perturbAmp(0.1F).perturbFreq(2.5F).build();

    /** The default noise settings used for the offset generator. */
    private static final NoiseMapSettings DEFAULT_OFFSET = NoiseMapSettings.builder()
        .frequency(0.01F).range(Range.of(10, 30)).build();

    /** Default spawn conditions for all burrow generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(10, 50)).build();

    /** Default decorator settings for all burrow generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    /** JSON key names. */
    public static final class Keys {
        public static final String conditions = "conditions";
        public static final String decorators = "decorators";
        public static final String map = "map";
        public static final String offset = "offset";
        public static final String radius = "radius";
        public static final String target = "target";
        public static final String stretch = "stretch";
        public static final String exponent = "exponent";
        public static final String shift = "shift";
        public static final String wallDistance = "wallDistance";
        public static final String wallExponent = "wallExponent";
        public static final String branches = "branches";

        private Keys() {}
    }

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
            .mapObject(Keys.map, o -> builder.map(NoiseMapSettings.from(o, DEFAULT_MAP)))
            .mapObject(Keys.offset, o -> builder.offset(NoiseMapSettings.from(o, DEFAULT_OFFSET)))
            .mapFloat(Keys.radius, builder::radius)
            .mapFloat(Keys.target, builder::target)
            .mapFloat(Keys.stretch, builder::stretch)
            .mapFloat(Keys.exponent, builder::exponent)
            .mapFloat(Keys.shift, builder::shift)
            .mapFloat(Keys.wallDistance, builder::wallDistance)
            .mapFloat(Keys.wallExponent, builder::wallExponent)
            // Branches inherit the conditions and decorators resolved for this burrow.
            .mapObject(Keys.branches, o -> builder.branches(Optional.of(
                TunnelSettings.from(o, builder.conditions, builder.decorators))))
            .release(builder::build);
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

        private Builder() {}

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
            return new BurrowSettings(this.conditions, this.decorators, this.map, this.offset,
                this.radius, this.target, this.stretch, this.exponent, this.shift,
                this.wallDistance, this.wallExponent, this.branches);
        }
    }
}
