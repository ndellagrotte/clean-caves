package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.noise.CachedNoiseGenerator;
import com.personthecat.cleancaves.noise.DummyGenerator;
import com.personthecat.cleancaves.util.HjsonMapper;
import fastnoise.FastNoise;
import fastnoise.FastNoise.NoiseType;
import org.hjson.JsonObject;

import net.minecraft.world.World;

import java.util.Optional;

/**
 * Generic info for spawning noise-based features in the world, for 2-dimensional noise only.
 * Most of these parameters are now merged directly into FastNoise; this type survives because it
 * documents exactly which of them a height map actually reads.
 */
public record NoiseMapSettings(Optional<Integer> seed,
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
        Optional<Float> dummyOutput) {

    public static Builder builder() {
        return new Builder();
    }

    public static NoiseMapSettings from(JsonObject json, NoiseMapSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static NoiseMapSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static NoiseMapSettings copyInto(JsonObject json, Builder builder) {
        return new HjsonMapper(json)
                .mapInt(Keys.SEED, i -> builder.seed(Optional.of(i)))
                .mapFloat(Keys.FREQUENCY, builder::frequency)
                .mapNoiseType(Keys.TYPE, builder::type)
                .mapFloat(Keys.PERTURB_AMP, builder::perturbAmp)
                .mapFloat(Keys.PERTURB_FREQ, builder::perturbFreq)
                .mapInt(Keys.OCTAVES, builder::octaves)
                .mapRange(Keys.RANGE, builder::range)
                .mapBool(Keys.PERTURB, builder::perturb)
                .mapBool(Keys.INVERT, builder::invert)
                .mapBool(Keys.CACHE, builder::cache)
                .mapBool(Keys.DUMMY, builder::dummy)
                .mapFloat(Keys.DUMMY_OUTPUT, f -> builder.dummyOutput(Optional.of(f)))
                .release(builder::build);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public FastNoise getGenerator(World world) {
        if (this.dummy) {
            return new DummyGenerator(this.dummyOutput.orElseGet(() -> (this.range.min() + this.range.max()) / 2.0F));
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

    /** JSON key names. */
    public static final class Keys {

        public static final String SEED = "seed";
        public static final String FREQUENCY = "frequency";
        public static final String TYPE = "type";
        public static final String PERTURB_AMP = "perturbAmp";
        public static final String PERTURB_FREQ = "perturbFreq";
        public static final String OCTAVES = "octaves";
        public static final String RANGE = "range";
        public static final String PERTURB = "perturb";
        public static final String INVERT = "invert";
        public static final String CACHE = "cache";
        public static final String DUMMY = "dummy";
        public static final String DUMMY_OUTPUT = "dummyOutput";

        private Keys() { }

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

        private Builder() { }

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
            return new NoiseMapSettings(this.seed, this.frequency, this.type, this.perturbAmp, this.perturbFreq,
                    this.octaves, this.range, this.perturb, this.invert, this.cache, this.dummy, this.dummyOutput);
        }

    }

}
