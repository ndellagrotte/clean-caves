package com.personthecat.cleancaves.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link Range} is inclusive-minimum, exclusive-maximum, and has a distinct empty case. */
class RangeTest {

    @Test
    @DisplayName("of() sorts its bounds")
    void sortsBounds() {
        final Range range = Range.of(9, 3);
        assertEquals(3, range.min());
        assertEquals(9, range.max());
    }

    @Test
    @DisplayName("contains is inclusive of min and exclusive of max")
    void containsIsHalfOpen() {
        final Range range = Range.of(3, 6);
        assertTrue(range.contains(3));
        assertTrue(range.contains(5));
        assertFalse(range.contains(6));
        assertFalse(range.contains(2));
    }

    @Test
    @DisplayName("iterating yields min up to but not including max")
    void iteratesHalfOpen() {
        final List<Integer> seen = new ArrayList<>();
        Range.of(2, 5).forEach(seen::add);
        assertEquals(List.of(2, 3, 4), seen);
    }

    @Test
    @DisplayName("checkedOrEmpty produces the empty range when the span is not usable")
    void checkedOrEmpty() {
        assertTrue(Range.checkedOrEmpty(5, 5).isEmpty());
        assertTrue(Range.checkedOrEmpty(9, 3).isEmpty());
        assertFalse(Range.checkedOrEmpty(3, 9).isEmpty());
    }

    @Test
    @DisplayName("the empty range yields nothing and contains nothing")
    void emptyRangeBehaviour() {
        final Range empty = Range.empty();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.diff());
        assertEquals(0, empty.rand(new Random(1)));
        assertFalse(empty.contains(0));
        assertFalse(empty.iterator().hasNext());
    }

    @Test
    @DisplayName("rand stays within bounds, inclusive on both ends")
    void randStaysInBounds() {
        final Range range = Range.of(4, 8);
        final Random rand = new Random(12345L);
        for (int i = 0; i < 200; i++) {
            final int value = range.rand(rand);
            assertTrue(value >= 4 && value <= 8, "out of bounds: " + value);
        }
    }

    @Test
    @DisplayName("a single-value range covers exactly that value")
    void singleValueRange() {
        final Range range = Range.of(7);
        assertEquals(7, range.min());
        assertEquals(7, range.max());
        assertEquals(7, range.rand(new Random(1)));
    }
}
