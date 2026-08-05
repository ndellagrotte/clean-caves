package com.personthecat.cleancaves.data;

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
 * Generic info for spawning noise-based features in the world, for 2-dimensional noise only.
 * Most of these parameters are now merged directly into FastNoise; this type survives because it
 * documents exactly which of them a height map actually reads.
 */
public record NoiseMapSettings(
    Optional<Integer> seed,
    float frequency,
    NoiseType type,
    float perturbAmp,
    float perturbFreq,
    int octaves,
    Range range,
    boolean perturb,
    boolean invert,
    boolean cache,
    boolean dummy,
    Optional<Float> dummyOutput
) {
    /** JSON key names. */
    public static final class Keys {
        public static final String seed = "seed";
        public static final String frequency = "frequency";
        public static final String type = "type";
        public static final String perturbAmp = "perturbAmp";
        public static final String perturbFreq = "perturbFreq";
        public static final String octaves = "octaves";
        public static final String range = "range";
        public static final String perturb = "perturb";
        public static final String invert = "invert";
        public static final String cache = "cache";
        public static final String dummy = "dummy";
        public static final String dummyOutput = "dummyOutput";

        private Keys() {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static NoiseMapSettings from(JsonObject json, NoiseMapSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static NoiseMapSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static NoiseMapSettings copyInto(JsonObject json, Builder builder) {
        return new HjsonMapper(json)
            .mapInt(Keys.seed, i -> builder.seed(Optional.of(i)))
            .mapFloat(Keys.frequency, builder::frequency)
            .mapNoiseType(Keys.type, builder::type)
            .mapFloat(Keys.perturbAmp, builder::perturbAmp)
            .mapFloat(Keys.perturbFreq, builder::perturbFreq)
            .mapInt(Keys.octaves, builder::octaves)
            .mapRange(Keys.range, builder::range)
            .mapBool(Keys.perturb, builder::perturb)
            .mapBool(Keys.invert, builder::invert)
            .mapBool(Keys.cache, builder::cache)
            .mapBool(Keys.dummy, builder::dummy)
            .mapFloat(Keys.dummyOutput, f -> builder.dummyOutput(Optional.of(f)))
            .release(builder::build);
    }

    public FastNoise getGenerator(World world) {
        if (this.dummy) {
            return new DummyGenerator(
                this.dummyOutput.orElseGet(() -> (this.range.min() + this.range.max()) / 2.0F));
        }
        final FastNoise noise = new FastNoise(NoiseSeeds.derive(world, this.seed))
            .SetNoiseType(this.type)
            .SetFrequency(this.frequency)
            .SetFractalOctaves(this.octaves)
            .SetRange(this.range.min(), this.range.max())
            .SetGradientPerturb(this.perturb)
            .SetGradientPerturbAmp(this.perturbAmp)
            .SetGradientPerturbFrequency(this.perturbFreq)
            .SetInterp(FastNoise.Interp.Hermite)
            .SetInvert(this.invert);
        return this.cache ? new CachedNoiseGenerator(noise) : noise;
    }

    public static final class Builder {
        private Optional<Integer> seed = Optional.empty();
        private float frequency = 1.0f;
        private NoiseType type = NoiseType.SimplexFractal;
        private float perturbAmp = 1.0F;
        private float perturbFreq = 1.0F;
        private int octaves = 1;
        private Range range = Range.of(-1, 1);
        private boolean perturb = false;
        private boolean invert = false;
        private boolean cache = false;
        private boolean dummy = false;
        private Optional<Float> dummyOutput = Optional.empty();

        private Builder() {}

        private Builder(NoiseMapSettings source) {
            this.seed = source.seed;
            this.frequency = source.frequency;
            this.type = source.type;
            this.perturbAmp = source.perturbAmp;
            this.perturbFreq = source.perturbFreq;
            this.octaves = source.octaves;
            this.range = source.range;
            this.perturb = source.perturb;
            this.invert = source.invert;
            this.cache = source.cache;
            this.dummy = source.dummy;
            this.dummyOutput = source.dummyOutput;
        }

        public Builder seed(Optional<Integer> seed) {
            this.seed = seed;
            return this;
        }

        public Builder frequency(float frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder type(NoiseType type) {
            this.type = type;
            return this;
        }

        public Builder perturbAmp(float perturbAmp) {
            this.perturbAmp = perturbAmp;
            return this;
        }

        public Builder perturbFreq(float perturbFreq) {
            this.perturbFreq = perturbFreq;
            return this;
        }

        public Builder octaves(int octaves) {
            this.octaves = octaves;
            return this;
        }

        public Builder range(Range range) {
            this.range = range;
            return this;
        }

        public Builder perturb(boolean perturb) {
            this.perturb = perturb;
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

        public Builder dummyOutput(Optional<Float> dummyOutput) {
            this.dummyOutput = dummyOutput;
            return this;
        }

        public NoiseMapSettings build() {
            return new NoiseMapSettings(this.seed, this.frequency, this.type, this.perturbAmp,
                this.perturbFreq, this.octaves, this.range, this.perturb, this.invert, this.cache,
                this.dummy, this.dummyOutput);
        }
    }
}
