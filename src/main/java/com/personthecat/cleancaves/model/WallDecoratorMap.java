package com.personthecat.cleancaves.model;

import com.personthecat.cleancaves.data.WallDecoratorSettings;

import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Wall decorators bucketed by the direction they apply to, so the carver can look up only the
 * decorators relevant to the face it is currently decorating.
 */
public record WallDecoratorMap(List<ConfiguredWallDecorator> all,
        List<ConfiguredWallDecorator> side,
        List<ConfiguredWallDecorator> up,
        List<ConfiguredWallDecorator> down,
        List<ConfiguredWallDecorator> north,
        List<ConfiguredWallDecorator> south,
        List<ConfiguredWallDecorator> east,
        List<ConfiguredWallDecorator> west,
        boolean containsAny) {

    public static Builder builder() {
        return new Builder();
    }

    public static WallDecoratorMap sort(List<WallDecoratorSettings> decorators, World world) {
        final Builder builder = builder().containsAny(!decorators.isEmpty());
        for (final WallDecoratorSettings cfg : decorators) {
            final ConfiguredWallDecorator wall = new ConfiguredWallDecorator(cfg, world);
            for (final Direction d : cfg.directions()) {
                switch (d) {
                    case ALL -> builder.all(wall);
                    case SIDE -> builder.side(wall);
                    case UP -> builder.up(wall);
                    case DOWN -> builder.down(wall);
                    case NORTH -> builder.north(wall);
                    case SOUTH -> builder.south(wall);
                    case EAST -> builder.east(wall);
                    case WEST -> builder.west(wall);
                    default -> throw new IllegalStateException("Unhandled direction: " + d);
                }
            }
        }
        return builder.build();
    }

    /** Each direction setter appends, mirroring the singular-adder style it replaces. */
    public static final class Builder {

        private final List<ConfiguredWallDecorator> all = new ArrayList<>();
        private final List<ConfiguredWallDecorator> side = new ArrayList<>();
        private final List<ConfiguredWallDecorator> up = new ArrayList<>();
        private final List<ConfiguredWallDecorator> down = new ArrayList<>();
        private final List<ConfiguredWallDecorator> north = new ArrayList<>();
        private final List<ConfiguredWallDecorator> south = new ArrayList<>();
        private final List<ConfiguredWallDecorator> east = new ArrayList<>();
        private final List<ConfiguredWallDecorator> west = new ArrayList<>();

        private boolean containsAny = false;

        private Builder() { }

        public Builder all(ConfiguredWallDecorator decorator) {
            this.all.add(decorator);
            return this;
        }

        public Builder side(ConfiguredWallDecorator decorator) {
            this.side.add(decorator);
            return this;
        }

        public Builder up(ConfiguredWallDecorator decorator) {
            this.up.add(decorator);
            return this;
        }

        public Builder down(ConfiguredWallDecorator decorator) {
            this.down.add(decorator);
            return this;
        }

        public Builder north(ConfiguredWallDecorator decorator) {
            this.north.add(decorator);
            return this;
        }

        public Builder south(ConfiguredWallDecorator decorator) {
            this.south.add(decorator);
            return this;
        }

        public Builder east(ConfiguredWallDecorator decorator) {
            this.east.add(decorator);
            return this;
        }

        public Builder west(ConfiguredWallDecorator decorator) {
            this.west.add(decorator);
            return this;
        }

        public Builder containsAny(boolean containsAny) {
            this.containsAny = containsAny;
            return this;
        }

        public WallDecoratorMap build() {
            return new WallDecoratorMap(List.copyOf(this.all), List.copyOf(this.side), List.copyOf(this.up),
                    List.copyOf(this.down), List.copyOf(this.north), List.copyOf(this.south), List.copyOf(this.east),
                    List.copyOf(this.west), this.containsAny);
        }

    }

}
