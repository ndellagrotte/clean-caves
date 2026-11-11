package com.personthecat.cleancaves.model;

import java.util.Arrays;
import java.util.List;

import static com.personthecat.cleancaves.util.CommonMethods.runExF;

public enum Direction {

    UP,
    DOWN,
    NORTH,
    SOUTH,
    EAST,
    WEST,
    SIDE,
    ALL;

    public static Direction from(String s) {
        return Arrays.stream(values())
                .filter(v -> v.toString().equalsIgnoreCase(s))
                .findFirst()
                .orElseThrow(() -> runExF("Error: Direction \"{}\" does not exist. The following are valid options:\n\n{}",
                        s, Arrays.toString(values())));
    }

    /** A DTO counterpart to using Direction arrays. */
    public record Container(boolean up,
            boolean down,
            boolean side,
            boolean north,
            boolean south,
            boolean east,
            boolean west) {

        private static final Container ALL_DIRECTIONS = new Container(true, true, true, true, true, true, true);

        /** The default when a preset names no directions at all, which is the same as an empty list. */
        public static Container all() {
            return ALL_DIRECTIONS;
        }

        /** Converts a direction list into a Direction.Container. An empty list means all. */
        public static Container from(List<Direction> directions) {
            if (directions.isEmpty()) {
                return ALL_DIRECTIONS;
            }
            boolean up = false;
            boolean down = false;
            boolean side = false;
            boolean north = false;
            boolean south = false;
            boolean east = false;
            boolean west = false;
            // Directions are assumed to be non-redundant from preset tester.
            for (final Direction direction : directions) {
                switch (direction) {
                    case UP -> up = true;
                    case DOWN -> down = true;
                    case SIDE -> side = true;
                    case NORTH -> north = true;
                    case SOUTH -> south = true;
                    case EAST -> east = true;
                    case WEST -> west = true;
                    case ALL -> {
                        return ALL_DIRECTIONS;
                    }
                    default -> throw new IllegalStateException("Unhandled direction: " + direction);
                }
            }
            return new Container(up, down, side, north, south, east, west);
        }

    }

}
