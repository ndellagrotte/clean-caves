package com.personthecat.cleancaves.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Column passes record a y coordinate once, and the merge steps read it back many times. */
class BlockMaskTest {

    @Test
    @DisplayName("a fresh mask contains nothing")
    void emptyByDefault() {
        final BlockMask mask = new BlockMask();
        assertThat(mask.isEmpty()).isTrue();
        for (int y = 0; y < 256; y++) {
            assertThat(mask.get(y)).as("y " + y).isFalse();
        }
    }

    @Test
    @DisplayName("every y coordinate round-trips through every word")
    void roundTripsEveryHeight() {
        final BlockMask mask = new BlockMask();
        for (int y = 0; y < 256; y++) {
            mask.set(y);
            assertThat(mask.get(y)).as("y " + y).isTrue();
            assertThat(mask.isEmpty()).isFalse();
            mask.clear();
            assertThat(mask.get(y)).as("cleared y " + y).isFalse();
        }
    }

    @Test
    @DisplayName("setting one coordinate leaves its neighbours alone")
    void doesNotBleed() {
        final BlockMask mask = new BlockMask();
        mask.set(63);
        mask.set(64);
        mask.set(255);
        assertThat(mask.get(63)).isTrue();
        assertThat(mask.get(64)).isTrue();
        assertThat(mask.get(255)).isTrue();
        assertThat(mask.get(62)).isFalse();
        assertThat(mask.get(65)).isFalse();
        assertThat(mask.get(0)).isFalse();
    }

    @Test
    @DisplayName("clear empties a populated mask")
    void clearEmpties() {
        final BlockMask mask = new BlockMask();
        for (int y = 0; y < 256; y += 7) {
            mask.set(y);
        }
        mask.clear();
        assertThat(mask.isEmpty()).isTrue();
        assertThat(mask.get(0)).isFalse();
        assertThat(mask.get(255)).isFalse();
    }

}
