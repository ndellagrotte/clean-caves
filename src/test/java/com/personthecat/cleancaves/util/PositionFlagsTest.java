package com.personthecat.cleancaves.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chunk-relative positions are packed into a single int to keep the carving loops allocation-free.
 * If the packing ever drifts, generators silently write blocks in the wrong place, so pin it down.
 */
class PositionFlagsTest {

    private static List<int[]> drain(PositionFlags flags) {
        final List<int[]> out = new ArrayList<>();
        flags.forEach((x, y, z) -> out.add(new int[] {x, y, z}));
        return out;
    }

    @Test
    @DisplayName("round-trips every corner of the chunk-relative coordinate space")
    void roundTripsBounds() {
        final PositionFlags flags = new PositionFlags(16);
        final int[][] corners = {
            {0, 0, 0}, {15, 0, 0}, {0, 255, 0}, {0, 0, 15},
            {15, 255, 15}, {7, 128, 9}, {1, 1, 1}
        };
        for (final int[] c : corners) {
            flags.add(c[0], c[1], c[2]);
        }
        final List<int[]> read = drain(flags);
        assertEquals(corners.length, read.size());
        for (int i = 0; i < corners.length; i++) {
            assertEquals(corners[i][0], read.get(i)[0], "x at " + i);
            assertEquals(corners[i][1], read.get(i)[1], "y at " + i);
            assertEquals(corners[i][2], read.get(i)[2], "z at " + i);
        }
    }

    @Test
    @DisplayName("filter keeps only the positions that pass")
    void filterRetainsMatches() {
        final PositionFlags flags = new PositionFlags(8);
        for (int y = 0; y < 8; y++) {
            flags.add(1, y, 2);
        }
        flags.filter((x, y, z) -> y % 2 == 0);
        final List<int[]> read = drain(flags);
        assertEquals(4, read.size());
        read.forEach(p -> assertEquals(0, p[1] % 2));
    }

    @Test
    @DisplayName("anyMatches short-circuits over the stored positions")
    void anyMatches() {
        final PositionFlags flags = new PositionFlags(4);
        flags.add(3, 40, 5);
        assertTrue(flags.anyMatches((x, y, z) -> y == 40));
        assertFalse(flags.anyMatches((x, y, z) -> y == 41));
    }

    @Test
    @DisplayName("reset makes the buffer reusable without reallocating")
    void resetClears() {
        final PositionFlags flags = new PositionFlags(4);
        flags.add(1, 2, 3);
        flags.reset();
        assertTrue(drain(flags).isEmpty());
        flags.add(4, 5, 6);
        assertEquals(1, drain(flags).size());
    }
}
