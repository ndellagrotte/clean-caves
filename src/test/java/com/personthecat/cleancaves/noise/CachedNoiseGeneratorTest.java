package com.personthecat.cleancaves.noise;

import fastnoise.FastNoise;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** A cached generator has to return exactly what the generator it wraps would have. */
class CachedNoiseGeneratorTest {

    private static FastNoise noise(int seed) {
        return new FastNoise(seed).SetFrequency(0.05F);
    }

    @AfterEach
    void clear() {
        CachedNoiseHelper.removeAll();
    }

    @Test
    @DisplayName("matches the wrapped generator inside the cached area, on first and repeated reads")
    void matchesInsideArea() {
        final FastNoise plain = noise(7);
        final FastNoise cached = new CachedNoiseGenerator(noise(7));
        CachedNoiseHelper.begin(-32, 48);
        for (int pass = 0; pass < 2; pass++) {
            for (int x = -32; x < -16; x++) {
                for (int z = 48; z < 64; z++) {
                    assertThat(cached.GetNoise(x, z)).isEqualTo(plain.GetNoise(x, z));
                    assertThat(cached.GetNoise(x, 40, z)).isEqualTo(plain.GetNoise(x, 40, z));
                }
            }
        }
    }

    @Test
    @DisplayName("a read outside the cached area neither uses nor overwrites a cached block")
    void ignoresOtherChunks() {
        final FastNoise plain = noise(7);
        final FastNoise cached = new CachedNoiseGenerator(noise(7));
        CachedNoiseHelper.begin(0, 0);

        // (21, 5) and (5, 5) share a slot when addressed by their position within a chunk.
        assertThat(cached.GetNoise(21, 30, 5)).isEqualTo(plain.GetNoise(21, 30, 5));
        assertThat(cached.GetNoise(5, 30, 5)).isEqualTo(plain.GetNoise(5, 30, 5));
        assertThat(cached.GetNoise(21, 30, 5)).isEqualTo(plain.GetNoise(21, 30, 5));

        assertThat(cached.GetNoise(21, 5)).isEqualTo(plain.GetNoise(21, 5));
        assertThat(cached.GetNoise(5, 5)).isEqualTo(plain.GetNoise(5, 5));
    }

    @Test
    @DisplayName("heights outside of the world are computed rather than wrapped onto another block")
    void ignoresOutOfRangeHeights() {
        final FastNoise plain = noise(7);
        final FastNoise cached = new CachedNoiseGenerator(noise(7));
        CachedNoiseHelper.begin(0, 0);
        assertThat(cached.GetNoise(3, 300, 3)).isEqualTo(plain.GetNoise(3, 300, 3));
        assertThat(cached.GetNoise(3, 44, 3)).isEqualTo(plain.GetNoise(3, 44, 3));
        assertThat(cached.GetNoise(3, -1, 3)).isEqualTo(plain.GetNoise(3, -1, 3));
    }

    @Test
    @DisplayName("nothing is cached once the area has ended, and a new area starts clean")
    void endsAndRestarts() {
        final FastNoise plain = noise(7);
        final FastNoise cached = new CachedNoiseGenerator(noise(7));
        CachedNoiseHelper.begin(0, 0);
        cached.GetNoise(5, 30, 5);
        CachedNoiseHelper.end();
        assertThat(cached.GetNoise(165, 30, 5)).isEqualTo(plain.GetNoise(165, 30, 5));

        CachedNoiseHelper.begin(160, 0);
        assertThat(cached.GetNoise(165, 30, 5)).isEqualTo(plain.GetNoise(165, 30, 5));
    }

    @Test
    @DisplayName("generators that differ only in seed do not share a cache")
    void separatesDifferentGenerators() {
        final FastNoise plain = noise(8);
        final FastNoise first = new CachedNoiseGenerator(noise(7));
        final FastNoise second = new CachedNoiseGenerator(noise(8));
        CachedNoiseHelper.begin(0, 0);
        first.GetNoise(5, 30, 5);
        assertThat(second.GetNoise(5, 30, 5)).isEqualTo(plain.GetNoise(5, 30, 5));
    }

}
