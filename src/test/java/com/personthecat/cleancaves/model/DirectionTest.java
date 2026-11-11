package com.personthecat.cleancaves.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Which surfaces a feature may attach to. */
class DirectionTest {

    @Test
    @DisplayName("naming no directions means every direction, whether the list is empty or absent")
    void emptyMeansAll() {
        final Direction.Container all = Direction.Container.from(List.of(Direction.ALL));
        assertThat(Direction.Container.from(List.of())).isEqualTo(all);
        assertThat(Direction.Container.all()).isEqualTo(all);
        assertThat(all.up() && all.down() && all.side()).isTrue();
    }

    @Test
    @DisplayName("named directions are the only ones set")
    void namedDirectionsOnly() {
        final Direction.Container container = Direction.Container.from(List.of(Direction.UP, Direction.EAST));
        assertThat(container.up()).isTrue();
        assertThat(container.east()).isTrue();
        assertThat(container.down()).isFalse();
        assertThat(container.side()).isFalse();
        assertThat(container.west()).isFalse();
    }

}
