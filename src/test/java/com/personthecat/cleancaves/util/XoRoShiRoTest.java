package com.personthecat.cleancaves.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** World generation is only repeatable if a seed always produces the same stream. */
class XoRoShiRoTest {

    @ParameterizedTest
    @ValueSource(longs = { 0L, 1L, -1L, 123456789L, Long.MAX_VALUE, Long.MIN_VALUE })
    @DisplayName("two generators built from the same seed produce the same stream")
    void sameSeedSameStream(long seed) {
        final Random first = new XoRoShiRo(seed);
        final Random second = new XoRoShiRo(seed);
        for (int i = 0; i < 16; i++) {
            assertThat(first.nextLong()).isEqualTo(second.nextLong());
        }
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, 42L, Long.MIN_VALUE })
    @DisplayName("reseeding with the same value restarts the stream")
    void reseedingRestarts(long seed) {
        final Random rand = new XoRoShiRo(seed);
        final long first = rand.nextLong();
        rand.nextLong();
        rand.setSeed(seed);
        assertThat(rand.nextLong()).isEqualTo(first);
    }

}
