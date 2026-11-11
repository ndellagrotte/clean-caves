package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.FloatRange;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.noise.CachedNoiseGenerator;
import com.personthecat.cleancaves.noise.DummyGenerator;
import com.personthecat.cleancaves.util.HjsonMapper;
import fastnoise.FastNoise;
import fastnoise.FastNoise.CellularDistanceFunction;
import fastnoise.FastNoise.CellularReturnType;
import fastnoise.FastNoise.FractalType;
import fastnoise.FastNoise.Interp;
import fastnoise.FastNoise.NoiseType;
import org.hjson.JsonObject;

import net.minecraft.world.World;

import java.util.Optional;

/**
 * A variant of {@link NoiseRegionSettings} which contains additional fields specifically relevant
 * to 3-dimensional noise generation. The two remain separate so that each can document exactly
 * which fields it reads.
 */
public record NoiseSettings(Optional<Integer> seed,
        float frequency,
        FloatRange threshold,
        float stretch,
        float lacunarity,
        float gain,
        float perturbAmp,
        float perturbFreq,
        float jitterX,
        float jitterY,
        float jitterZ,
        int octaves,
        int offset,
        boolean perturb,
        boolean invert,
        boolean cache,
        boolean dummy,
        float dummyOutput,
        Interp interp,
        NoiseType type,
        FractalType fractal,
        CellularDistanceFunction distFunc,
        CellularReturnType returnType,
        NoiseType cellularLookup) {

    public static Builder builder() {
        return new Builder();
    }

    public static NoiseSettings from(JsonObject json, NoiseSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static NoiseSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static NoiseSettings copyInto(JsonObject json, Builder builder) {
        return new HjsonMapper(json)
                .mapInt(Keys.SEED, i -> builder.seed(Optional.of(i)))
                .mapFloat(Keys.FREQUENCY, builder::frequency)
                .mapFloatRange(Keys.THRESHOLD, builder::threshold)
                .mapFloat(Keys.STRETCH, builder::stretch)
                .mapFloat(Keys.LACUNARITY, builder::lacunarity)
                .mapFloat(Keys.GAIN, builder::gain)
                .mapFloat(Keys.PERTURB_AMP, builder::perturbAmp)
                .mapFloat(Keys.PERTURB_FREQ, builder::perturbFreq)
                .mapFloat(Keys.JITTER, i -> builder.jitterX(i).jitterY(i).jitterZ(i))
                .mapFloat(Keys.JITTER_X, builder::jitterX)
                .mapFloat(Keys.JITTER_Y, builder::jitterY)
                .mapFloat(Keys.JITTER_Z, builder::jitterZ)
                .mapInt(Keys.OCTAVES, builder::octaves)
                .mapInt(Keys.OFFSET, builder::offset)
                .mapBool(Keys.PERTURB, builder::perturb)
                .mapBool(Keys.INVERT, builder::invert)
                .mapBool(Keys.CACHE, builder::cache)
                .mapBool(Keys.DUMMY, builder::dummy)
                .mapFloat(Keys.DUMMY_OUTPUT, builder::dummyOutput)
                .mapInterp(Keys.INTERP, builder::interp)
                .mapNoiseType(Keys.TYPE, builder::type)
                .mapFractalType(Keys.FRACTAL, builder::fractal)
                .mapDistFunc(Keys.DIST_FUNC, builder::distFunc)
                .mapReturnType(Keys.RETURN_TYPE, builder::returnType)
                .mapNoiseType(Keys.CELLULAR_LOOKUP, builder::cellularLookup)
                .release(builder::build);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    /** Converts these settings into a regular {@link FastNoise} object. */
    public FastNoise getGenerator(World world) {
        if (this.dummy) {
            return new DummyGenerator(this.dummyOutput);
        }
        final FastNoise noise = new FastNoise(NoiseSeeds.derive(world, this.seed))
                .SetNoiseType(this.type)
                .SetFrequency(this.frequency)
                .SetFractalType(this.fractal)
                .SetFractalOctaves(this.octaves)
                .SetInvert(this.invert)
                .SetFractalGain(this.gain)
                .SetCellularReturnType(this.returnType)
                .SetGradientPerturbAmp(this.perturbAmp)
                .SetGradientPerturbFrequency(this.perturbFreq)
                .SetGradientPerturb(this.perturb)
                .SetFractalLacunarity(this.lacunarity)
                .SetCellularNoiseLookup(this.cellularLookup)
                .SetCellularDistanceFunction(this.distFunc)
                .SetInterp(this.interp)
                .SetCellularJitterX(this.jitterX)
                .SetCellularJitterY(this.jitterY)
                .SetCellularJitterZ(this.jitterZ)
                .SetThreshold(this.threshold.min(), this.threshold.max())
                .SetStretch(this.stretch)
                .SetOffset(this.offset);
        return this.cache ? new CachedNoiseGenerator(noise) : noise;
    }

    /** JSON key names. */
    public static final class Keys {

        public static final String SEED = "seed";
        public static final String FREQUENCY = "frequency";
        public static final String THRESHOLD = "threshold";
        public static final String STRETCH = "stretch";
        public static final String LACUNARITY = "lacunarity";
        public static final String GAIN = "gain";
        public static final String PERTURB_AMP = "perturbAmp";
        public static final String PERTURB_FREQ = "perturbFreq";
        /** Shorthand which sets all three axes at once. */
        public static final String JITTER = "jitter";
        public static final String JITTER_X = "jitterX";
        public static final String JITTER_Y = "jitterY";
        public static final String JITTER_Z = "jitterZ";
        public static final String OCTAVES = "octaves";
        public static final String OFFSET = "offset";
        public static final String PERTURB = "perturb";
        public static final String INVERT = "invert";
        public static final String CACHE = "cache";
        public static final String DUMMY = "dummy";
        public static final String DUMMY_OUTPUT = "dummyOutput";
        public static final String INTERP = "interp";
        public static final String TYPE = "type";
        public static final String FRACTAL = "fractal";
        public static final String DIST_FUNC = "distFunc";
        public static final String RETURN_TYPE = "returnType";
        public static final String CELLULAR_LOOKUP = "cellularLookup";

        private Keys() { }

    }

    public static final class Builder {

        private Optional<Integer> seed = Optional.empty();
        private float frequency = 1.0F;
        private FloatRange threshold = Range.of(0.0F);
        private float stretch = 1.0F;
        private float lacunarity = 1.0F;
        private float gain = 0.5F;
        private float perturbAmp = 1.0F;
        private float perturbFreq = 1.0F;
        private float jitterX = 0.45F;
        private float jitterY = 0.45F;
        private float jitterZ = 0.45F;
        private int octaves = 1;
        private int offset = 0;
        private boolean perturb = false;
        private boolean invert = false;
        private boolean cache = false;
        private boolean dummy = false;
        private float dummyOutput = 1.0F;
        private Interp interp = Interp.Hermite;
        private NoiseType type = NoiseType.SimplexFractal;
        private FractalType fractal = FractalType.FBM;
        private CellularDistanceFunction distFunc = CellularDistanceFunction.Euclidean;
        private CellularReturnType returnType = CellularReturnType.Distance2;
        private NoiseType cellularLookup = NoiseType.Simplex;

        private Builder() { }

        private Builder(NoiseSettings source) {
            this.seed = source.seed;
            this.frequency = source.frequency;
            this.threshold = source.threshold;
            this.stretch = source.stretch;
            this.lacunarity = source.lacunarity;
            this.gain = source.gain;
            this.perturbAmp = source.perturbAmp;
            this.perturbFreq = source.perturbFreq;
            this.jitterX = source.jitterX;
            this.jitterY = source.jitterY;
            this.jitterZ = source.jitterZ;
            this.octaves = source.octaves;
            this.offset = source.offset;
            this.perturb = source.perturb;
            this.invert = source.invert;
            this.cache = source.cache;
            this.dummy = source.dummy;
            this.dummyOutput = source.dummyOutput;
            this.interp = source.interp;
            this.type = source.type;
            this.fractal = source.fractal;
            this.distFunc = source.distFunc;
            this.returnType = source.returnType;
            this.cellularLookup = source.cellularLookup;
        }

        public Builder seed(Optional<Integer> seed) {
            this.seed = seed;
            return this;
        }

        public Builder frequency(float frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder threshold(FloatRange threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder stretch(float stretch) {
            this.stretch = stretch;
            return this;
        }

        public Builder lacunarity(float lacunarity) {
            this.lacunarity = lacunarity;
            return this;
        }

        public Builder gain(float gain) {
            this.gain = gain;
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

        public Builder jitterX(float jitterX) {
            this.jitterX = jitterX;
            return this;
        }

        public Builder jitterY(float jitterY) {
            this.jitterY = jitterY;
            return this;
        }

        public Builder jitterZ(float jitterZ) {
            this.jitterZ = jitterZ;
            return this;
        }

        public Builder octaves(int octaves) {
            this.octaves = octaves;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
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

        public Builder dummyOutput(float dummyOutput) {
            this.dummyOutput = dummyOutput;
            return this;
        }

        public Builder interp(Interp interp) {
            this.interp = interp;
            return this;
        }

        public Builder type(NoiseType type) {
            this.type = type;
            return this;
        }

        public Builder fractal(FractalType fractal) {
            this.fractal = fractal;
            return this;
        }

        public Builder distFunc(CellularDistanceFunction distFunc) {
            this.distFunc = distFunc;
            return this;
        }

        public Builder returnType(CellularReturnType returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder cellularLookup(NoiseType cellularLookup) {
            this.cellularLookup = cellularLookup;
            return this;
        }

        public NoiseSettings build() {
            return new NoiseSettings(this.seed, this.frequency, this.threshold, this.stretch, this.lacunarity,
                    this.gain, this.perturbAmp, this.perturbFreq, this.jitterX, this.jitterY, this.jitterZ,
                    this.octaves, this.offset, this.perturb, this.invert, this.cache, this.dummy, this.dummyOutput,
                    this.interp, this.type, this.fractal, this.distFunc, this.returnType, this.cellularLookup);
        }

    }

}
