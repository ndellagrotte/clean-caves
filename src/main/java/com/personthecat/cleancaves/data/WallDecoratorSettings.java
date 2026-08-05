package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.Direction;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.personthecat.cleancaves.util.CommonMethods.runExF;

public record WallDecoratorSettings(
    List<IBlockState> states,
    double integrity,
    Range height,
    List<Direction> directions,
    List<IBlockState> matchers,
    Placement placement,
    Optional<NoiseSettings> noise
) {
    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = DecoratorSettings.Keys.wallDecorators;

    /** The default noise values for WallDecorators with noise. */
    public static final NoiseSettings DEFAULT_NOISE = NoiseSettings.builder()
        .frequency(0.02f).threshold(Range.of(0.0F)).stretch(1.0f).build();

    /** JSON key names. */
    public static final class Keys {
        public static final String states = "states";
        public static final String integrity = "integrity";
        public static final String height = "height";
        public static final String directions = "directions";
        public static final String matchers = "matchers";
        public static final String placement = "placement";
        public static final String noise = "noise";

        private Keys() {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public static WallDecoratorSettings from(JsonObject json) {
        final Builder builder = builder();
        return new HjsonMapper(json)
            .mapRequiredStateList(Keys.states, FEATURE_NAME, builder::states)
            .mapObject(Keys.noise, o -> builder.noise(Optional.of(NoiseSettings.from(o, DEFAULT_NOISE))))
            .mapFloat(Keys.integrity, builder::integrity)
            .mapRange(Keys.height, builder::height)
            .mapDirectionList(Keys.directions, builder::directions)
            .mapStateList(Keys.matchers, builder::matchers)
            .mapPlacementPreference(Keys.placement, builder::placement)
            .release(builder::build);
    }

    /** Indicates whether to place blocks inside of or on top of a wall. */
    public enum Placement {
        OVERLAY,
        EMBED;

        public static Placement from(String s) {
            return Arrays.stream(values())
                .filter(v -> v.toString().equalsIgnoreCase(s))
                .findFirst()
                .orElseThrow(() -> runExF(
                    "Error: Preference \"{}\" does not exist. The following are valid options:\n\n{}",
                    s, Arrays.toString(values())));
        }
    }

    public static final class Builder {
        private List<IBlockState> states = List.of();
        private double integrity = 1.0;
        private Range height = Range.of(10, 50);
        private List<Direction> directions = List.of(Direction.ALL);
        private List<IBlockState> matchers = List.of(Blocks.STONE.getDefaultState());
        private Placement placement = Placement.EMBED;
        private Optional<NoiseSettings> noise = Optional.empty();

        private Builder() {}

        public Builder states(List<IBlockState> states) {
            this.states = states;
            return this;
        }

        public Builder integrity(double integrity) {
            this.integrity = integrity;
            return this;
        }

        public Builder height(Range height) {
            this.height = height;
            return this;
        }

        public Builder directions(List<Direction> directions) {
            this.directions = directions;
            return this;
        }

        public Builder matchers(List<IBlockState> matchers) {
            this.matchers = matchers;
            return this;
        }

        public Builder placement(Placement placement) {
            this.placement = placement;
            return this;
        }

        public Builder noise(Optional<NoiseSettings> noise) {
            this.noise = noise;
            return this;
        }

        public WallDecoratorSettings build() {
            return new WallDecoratorSettings(this.states, this.integrity, this.height,
                this.directions, this.matchers, this.placement, this.noise);
        }
    }
}
