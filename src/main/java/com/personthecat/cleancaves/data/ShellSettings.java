package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import java.util.List;
import java.util.Optional;

/** A variant of wall decorators that can spawn multiple layers deep without directionality. */
public record ShellSettings(double radius,
        int sphereResolution,
        Optional<Float> noiseThreshold,
        List<Decorator> decorators) {

    public static Builder builder() {
        return new Builder();
    }

    public static ShellSettings from(JsonObject json) {
        final Builder builder = builder();
        return new HjsonMapper(json)
                .mapFloat(Keys.RADIUS, builder::radius)
                .mapInt(Keys.SPHERE_RESOLUTION, builder::sphereResolution)
                .mapFloat(Keys.NOISE_THRESHOLD, f -> builder.noiseThreshold(Optional.of(f)))
                .mapArray(Keys.DECORATORS, Decorator::from, builder::decorators)
                .release(builder::build);
    }

    /** JSON key names. */
    public static final class Keys {

        public static final String RADIUS = "radius";
        public static final String SPHERE_RESOLUTION = "sphereResolution";
        public static final String NOISE_THRESHOLD = "noiseThreshold";
        public static final String DECORATORS = "decorators";

        private Keys() { }

    }

    public static final class Builder {

        private double radius = 0.0;
        private int sphereResolution = 2;
        private Optional<Float> noiseThreshold = Optional.empty();
        private List<Decorator> decorators = List.of();

        private Builder() { }

        public Builder radius(double radius) {
            this.radius = radius;
            return this;
        }

        public Builder sphereResolution(int sphereResolution) {
            this.sphereResolution = sphereResolution;
            return this;
        }

        public Builder noiseThreshold(Optional<Float> noiseThreshold) {
            this.noiseThreshold = noiseThreshold;
            return this;
        }

        public Builder decorators(List<Decorator> decorators) {
            this.decorators = decorators;
            return this;
        }

        public ShellSettings build() {
            return new ShellSettings(this.radius, this.sphereResolution, this.noiseThreshold, this.decorators);
        }

    }

    /** A single block-and-conditions entry within a shell. */
    public record Decorator(List<IBlockState> states,
            List<IBlockState> matchers,
            Range height,
            double integrity,
            Optional<NoiseSettings> noise) {

        /** The default noise values for shell decorators with noise. */
        public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
                .frequency(0.02f)
                .threshold(Range.of(-0.8F))
                .stretch(1.0f)
                .octaves(1)
                .build();

        public static Builder builder() {
            return new Builder();
        }

        private static Decorator from(JsonObject json) {
            final Builder builder = builder();
            return new HjsonMapper(json)
                    .mapRequiredStateList(Keys.STATES, ShellSettings.Keys.DECORATORS, builder::states)
                    .mapStateList(Keys.MATCHERS, builder::matchers)
                    .mapRange(Keys.HEIGHT, builder::height)
                    .mapFloat(Keys.INTEGRITY, builder::integrity)
                    .mapObject(Keys.NOISE, o -> builder.noise(Optional.of(NoiseSettings.from(o, DEFAULT_NOISE))))
                    .release(builder::build);
        }

        /** JSON key names. */
        public static final class Keys {

            public static final String STATES = "states";
            public static final String MATCHERS = "matchers";
            public static final String HEIGHT = "height";
            public static final String INTEGRITY = "integrity";
            public static final String NOISE = "noise";

            private Keys() { }

        }

        public static final class Builder {

            private List<IBlockState> states = List.of();
            private List<IBlockState> matchers = List.of(Blocks.STONE.getDefaultState());
            private Range height = Range.of(0, 63);
            private double integrity = 1.0;
            private Optional<NoiseSettings> noise = Optional.empty();

            private Builder() { }

            public Builder states(List<IBlockState> states) {
                this.states = states;
                return this;
            }

            public Builder matchers(List<IBlockState> matchers) {
                this.matchers = matchers;
                return this;
            }

            public Builder height(Range height) {
                this.height = height;
                return this;
            }

            public Builder integrity(double integrity) {
                this.integrity = integrity;
                return this;
            }

            public Builder noise(Optional<NoiseSettings> noise) {
                this.noise = noise;
                return this;
            }

            public Decorator build() {
                return new Decorator(this.states, this.matchers, this.height, this.integrity, this.noise);
            }

        }

    }

}
