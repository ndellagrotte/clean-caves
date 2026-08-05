package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.FloatRange;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.noise.CachedNoiseGenerator;
import com.personthecat.cleancaves.noise.DummyGenerator;
import com.personthecat.cleancaves.util.HjsonMapper;
import fastnoise.FastNoise;
import fastnoise.FastNoise.NoiseType;
import net.minecraft.world.World;
import org.hjson.JsonObject;

import java.util.Optional;

/**
 * Everything needed for spawning features in noise-based regions in the world. Separate from
 * {@link NoiseSettings} because it defines exactly which fields a {@code region} object reads,
 * which is what lets {@link com.personthecat.cleancaves.config.PresetTester} tell users about
 * settings it will ignore.
 */
public record NoiseRegionSettings(
    Optional<Integer> seed,
    FloatRange threshold,
    float frequency,
    boolean invert,
    boolean cache,
    boolean dummy,
    float dummyOutput,
    NoiseType type
) {
    /** JSON key names. */
    public static final class Keys {
        public static final String seed = "seed";
        public static final String threshold = "threshold";
        public static final String frequency = "frequency";
        public static final String invert = "invert";
        public static final String cache = "cache";
        public static final String dummy = "dummy";
        public static final String dummyOutput = "dummyOutput";
        public static final String type = "type";

        private Keys() {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static NoiseRegionSettings from(JsonObject json, NoiseRegionSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static NoiseRegionSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static NoiseRegionSettings copyInto(JsonObject json, Builder builder) {
        return new HjsonMapper(json)
            .mapInt(Keys.seed, i -> builder.seed(Optional.of(i)))
            .mapFloat(Keys.frequency, builder::frequency)
            .mapFloatRange(Keys.threshold, builder::threshold)
            .mapBool(Keys.cache, builder::cache)
            .mapBool(Keys.dummy, builder::dummy)
            .mapFloat(Keys.dummyOutput, builder::dummyOutput)
            .mapNoiseType(Keys.type, builder::type)
            .release(builder::build);
    }

    public FastNoise getGenerator(World world) {
        if (this.dummy) {
            return new DummyGenerator(this.dummyOutput);
        }
        final FastNoise noise = new FastNoise(NoiseSeeds.derive(world, this.seed))
            .SetNoiseType(this.type)
            .SetThreshold(this.threshold.min(), this.threshold.max())
            .SetFrequency(this.frequency)
            .SetInvert(this.invert)
            .SetInterp(FastNoise.Interp.Hermite);
        return this.cache ? new CachedNoiseGenerator(noise) : noise;
    }

    public static final class Builder {
        private Optional<Integer> seed = Optional.empty();
        private FloatRange threshold = Range.of(0.0F);
        private float frequency = 1.0f;
        private boolean invert = false;
        private boolean cache = false;
        private boolean dummy = false;
        private float dummyOutput = 1.0F;
        private NoiseType type = NoiseType.SimplexFractal;

        private Builder() {}

        private Builder(NoiseRegionSettings source) {
            this.seed = source.seed;
            this.threshold = source.threshold;
            this.frequency = source.frequency;
            this.invert = source.invert;
            this.cache = source.cache;
            this.dummy = source.dummy;
            this.dummyOutput = source.dummyOutput;
            this.type = source.type;
        }

        public Builder seed(Optional<Integer> seed) {
            this.seed = seed;
            return this;
        }

        public Builder threshold(FloatRange threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder frequency(float frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder invert(boolean invert) {
            this.invert = invert;
            return this;
        }

        public Builder cache(boolean cache) {
            this.cache = cache;
            return this;
        }

        public Builder dummy(boolean dummy) {
            this.dummy = dummy;
            return this;
        }

        public Builder dummyOutput(float dummyOutput) {
            this.dummyOutput = dummyOutput;
            return this;
        }

        public Builder type(NoiseType type) {
            this.type = type;
            return this;
        }

        public NoiseRegionSettings build() {
            return new NoiseRegionSettings(this.seed, this.threshold, this.frequency, this.invert,
                this.cache, this.dummy, this.dummyOutput, this.type);
        }
    }
}
