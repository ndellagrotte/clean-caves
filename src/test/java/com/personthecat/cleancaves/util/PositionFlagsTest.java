package com.personthecat.cleancaves.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chunk-relative positions are packed into a single int to keep the carving loops allocation-free.
 * If the packing ever drifts, generators silently write blocks in the wrong place, so pin it down.
 */
class PositionFlagsTest {

    private static List<int[]> drain(PositionFlags flags) {
        final List<int[]> out = new ArrayList<>();
        flags.forEach((x, y, z) -> out.add(new int[] { x, y, z }));
        return out;
    }

    @Test
    @DisplayName("round-trips every corner of the chunk-relative coordinate space")
    void roundTripsBounds() {
        final PositionFlags flags = new PositionFlags(16);
        final int[][] corners = {
                { 0, 0, 0 }, { 15, 0, 0 }, { 0, 255, 0 }, { 0, 0, 15 }, { 15, 255, 15 }, { 7, 128, 9 }, { 1, 1, 1 }
        };
        for (final int[] c : corners) {
            flags.add(c[0], c[1], c[2]);
        }
        final List<int[]> read = drain(flags);
        assertThat(read.size()).isEqualTo(corners.length);
        for (int i = 0; i < corners.length; i++) {
            assertThat(read.get(i)[0]).as("x at " + i).isEqualTo(corners[i][0]);
            assertThat(read.get(i)[1]).as("y at " + i).isEqualTo(corners[i][1]);
            assertThat(read.get(i)[2]).as("z at " + i).isEqualTo(corners[i][2]);
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
        assertThat(read.size()).isEqualTo(4);
        read.forEach(p -> assertThat(p[1] % 2).isEqualTo(0));
    }

    @Test
    @DisplayName("anyMatches short-circuits over the stored positions")
    void anyMatches() {
        final PositionFlags flags = new PositionFlags(4);
        flags.add(3, 40, 5);
        assertThat(flags.anyMatches((x, y, z) -> y == 40)).isTrue();
        assertThat(flags.anyMatches((x, y, z) -> y == 41)).isFalse();
    }

    @Test
    @DisplayName("addAll merges bands in the order they are appended, growing when needed")
    void addAllKeepsOrder() {
        final PositionFlags flags = new PositionFlags(1);
        for (int band = 0; band < 3; band++) {
            final PositionFlags part = new PositionFlags(4);
            for (int z = 0; z < 4; z++) {
                part.add(band, z, z);
            }
            flags.addAll(part);
        }
        final List<int[]> read = drain(flags);
        assertThat(read).hasSize(12);
        for (int i = 0; i < read.size(); i++) {
            assertThat(read.get(i)[0]).as("x at " + i).isEqualTo(i / 4);
            assertThat(read.get(i)[1]).as("y at " + i).isEqualTo(i % 4);
        }
    }

    @Test
    @DisplayName("addAll of an empty list leaves the target untouched")
    void addAllEmpty() {
        final PositionFlags flags = new PositionFlags(2);
        flags.add(1, 2, 3);
        flags.addAll(new PositionFlags(2));
        assertThat(drain(flags)).hasSize(1);
    }

    @Test
    @DisplayName("reset makes the buffer reusable without reallocating")
    void resetClears() {
        final PositionFlags flags = new PositionFlags(4);
        flags.add(1, 2, 3);
        flags.reset();
        assertThat(drain(flags).isEmpty()).isTrue();
        flags.add(4, 5, 6);
        assertThat(drain(flags).size()).isEqualTo(1);
    }

}
