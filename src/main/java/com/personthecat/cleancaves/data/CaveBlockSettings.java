package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import net.minecraft.block.state.IBlockState;
import org.hjson.JsonObject;

import java.util.List;
import java.util.Optional;

/** Contains all of the data needed for spawning alternative blocks in caves. */
public record CaveBlockSettings(
    List<IBlockState> states,
    double integrity,
    Range height,
    Optional<NoiseSettings> noise
) {
    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = DecoratorSettings.Keys.caveBlocks;

    /** The default noise values for CaveBlocks with noise. */
    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
        .frequency(0.02f).threshold(Range.of(-0.8F)).stretch(1.0f).octaves(1).build();

    /** JSON key names. */
    public static final class Keys {
        public static final String states = "states";
        public static final String integrity = "integrity";
        public static final String height = "height";
        public static final String noise = "noise";

        private Keys() {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CaveBlockSettings from(JsonObject json) {
        final Builder builder = builder();
        return new HjsonMapper(json)
            .mapRequiredStateList(Keys.states, FEATURE_NAME, builder::states)
            .mapObject(Keys.noise, o -> builder.noise(Optional.of(NoiseSettings.from(o, DEFAULT_NOISE))))
            .mapFloat(Keys.integrity, builder::integrity)
            .mapRange(Keys.height, builder::height)
            .release(builder::build);
    }

    public static final class Builder {
        private List<IBlockState> states = List.of();
        private double integrity = 1.0;
        private Range height = Range.of(0, 50);
        private Optional<NoiseSettings> noise = Optional.empty();

        private Builder() {}

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

        public Builder noise(Optional<NoiseSettings> noise) {
            this.noise = noise;
            return this;
        }

        public CaveBlockSettings build() {
            return new CaveBlockSettings(this.states, this.integrity, this.height, this.noise);
        }
    }
}
