package com.personthecat.cleancaves.world.generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Decoration is rolled per chunk, so every chunk needs a random stream of its own. */
class PrimerContextTest {

    @Test
    @DisplayName("mirrored chunks and chunks along the diagonal get different seeds")
    void mirroredChunksDiffer() {
        final long seed = 8675309L;
        assertThat(PrimerContext.chunkSeed(seed, 3, 7)).isNotEqualTo(PrimerContext.chunkSeed(seed, 7, 3));
        assertThat(PrimerContext.chunkSeed(seed, 5, 5)).isNotEqualTo(PrimerContext.chunkSeed(seed, 9, 9));
        assertThat(PrimerContext.chunkSeed(seed, -4, 4)).isNotEqualTo(PrimerContext.chunkSeed(seed, 4, -4));
    }

    @Test
    @DisplayName("no two chunks in a region share a seed")
    void seedsAreUniqueAcrossRegion() {
        final Set<Long> seeds = new HashSet<>();
        for (int x = -64; x < 64; x++) {
            for (int z = -64; z < 64; z++) {
                seeds.add(PrimerContext.chunkSeed(1L, x, z));
            }
        }
        assertThat(seeds).hasSize(128 * 128);
    }

}
