package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import java.util.List;
import java.util.Optional;

/** A variant of wall decorators which can spawn multiple layers deep on the floor only. */
public record PondSettings(List<IBlockState> states,
        double integrity,
        Range height,
        int depth,
        List<IBlockState> matchers,
        Optional<NoiseSettings> noise) {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = DecoratorSettings.Keys.PONDS;

    /** The default noise values for ponds with noise. */
    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
            .frequency(0.02f)
            .threshold(Range.of(0.0F))
            .stretch(1.0f)
            .build();

    /** JSON key names. */
    public static final class Keys {

        public static final String STATES = "states";
        public static final String INTEGRITY = "integrity";
        public static final String HEIGHT = "height";
        public static final String DEPTH = "depth";
        public static final String MATCHERS = "matchers";
        public static final String NOISE = "noise";

        private Keys() { }

    }

    public static Builder builder() {
        return new Builder();
    }

    public static PondSettings from(JsonObject json) {
        final Builder builder = builder();
        return new HjsonMapper(json)
                .mapRequiredStateList(Keys.STATES, FEATURE_NAME, builder::states)
                .mapObject(Keys.NOISE, o -> builder.noise(Optional.of(NoiseSettings.from(o, DEFAULT_NOISE))))
                .mapFloat(Keys.INTEGRITY, builder::integrity)
                .mapRange(Keys.HEIGHT, builder::height)
                .mapInt(Keys.DEPTH, builder::depth)
                .mapStateList(Keys.MATCHERS, builder::matchers)
                .release(builder::build);
    }

    public static final class Builder {

        private List<IBlockState> states = List.of();
        private double integrity = 1.0;
        private Range height = Range.of(10, 50);
        private int depth = 2;
        private List<IBlockState> matchers = List.of(Blocks.STONE.getDefaultState());
        private Optional<NoiseSettings> noise = Optional.empty();

        private Builder() { }

        public Builder states(List<IBlockState> states) {
            this.states = states;
            return this;
        }

        public Builder integrity(double integrity) {
            this.integrity = integrity;
            return this;
        }

        public Builder height(Range height) {
            this.height = height;
            return this;
        }

        public Builder depth(int depth) {
            this.depth = depth;
            return this;
        }

        public Builder matchers(List<IBlockState> matchers) {
            this.matchers = matchers;
            return this;
        }

        public Builder noise(Optional<NoiseSettings> noise) {
            this.noise = noise;
            return this;
        }

        public PondSettings build() {
            return new PondSettings(this.states, this.integrity, this.height, this.depth, this.matchers, this.noise);
        }

    }

}
