package com.personthecat.cleancaves.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link Range} is inclusive-minimum, exclusive-maximum, and has a distinct empty case. */
class RangeTest {

    @Test
    @DisplayName("of() sorts its bounds")
    void sortsBounds() {
        final Range range = Range.of(9, 3);
        assertThat(range.min()).isEqualTo(3);
        assertThat(range.max()).isEqualTo(9);
    }

    @Test
    @DisplayName("contains is inclusive of min and exclusive of max")
    void containsIsHalfOpen() {
        final Range range = Range.of(3, 6);
        assertThat(range.contains(3)).isTrue();
        assertThat(range.contains(5)).isTrue();
        assertThat(range.contains(6)).isFalse();
        assertThat(range.contains(2)).isFalse();
    }

    @Test
    @DisplayName("iterating yields min up to but not including max")
    void iteratesHalfOpen() {
        final List<Integer> seen = new ArrayList<>();
        Range.of(2, 5).forEach(seen::add);
        assertThat(seen).isEqualTo(List.of(2, 3, 4));
    }

    @Test
    @DisplayName("checkedOrEmpty produces the empty range when the span is not usable")
    void checkedOrEmpty() {
        assertThat(Range.checkedOrEmpty(5, 5).isEmpty()).isTrue();
        assertThat(Range.checkedOrEmpty(9, 3).isEmpty()).isTrue();
        assertThat(Range.checkedOrEmpty(3, 9).isEmpty()).isFalse();
    }

    @Test
    @DisplayName("the empty range yields nothing and contains nothing")
    void emptyRangeBehaviour() {
        final Range empty = Range.empty();
        assertThat(empty.isEmpty()).isTrue();
        assertThat(empty.diff()).isEqualTo(0);
        assertThat(empty.rand(new Random(1))).isEqualTo(0);
        assertThat(empty.contains(0)).isFalse();
        assertThat(empty.iterator().hasNext()).isFalse();
    }

    @Test
    @DisplayName("rand stays within bounds, inclusive on both ends")
    void randStaysInBounds() {
        final Range range = Range.of(4, 8);
        final Random rand = new Random(12345L);
        for (int i = 0; i < 200; i++) {
            final int value = range.rand(rand);
            assertThat(value >= 4 && value <= 8).as("out of bounds: " + value).isTrue();
        }
    }

    @Test
    @DisplayName("a single-value range covers exactly that value")
    void singleValueRange() {
        final Range range = Range.of(7);
        assertThat(range.min()).isEqualTo(7);
        assertThat(range.max()).isEqualTo(7);
        assertThat(range.rand(new Random(1))).isEqualTo(7);
    }

}
