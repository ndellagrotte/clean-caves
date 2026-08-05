package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import net.minecraft.world.biome.Biome;
import org.hjson.JsonObject;

import java.util.List;
import java.util.Optional;

/** Everything that decides whether a feature may spawn at a given place. */
public record ConditionSettings(
    boolean blacklistBiomes,
    List<Biome> biomes,
    boolean blacklistDimensions,
    List<Integer> dimensions,
    Range height,
    Optional<NoiseMapSettings> floor,
    Optional<NoiseMapSettings> ceiling,
    Optional<NoiseRegionSettings> region,
    Optional<NoiseSettings> noise
) {
    /** JSON key names. */
    public static final class Keys {
        public static final String blacklistBiomes = "blacklistBiomes";
        public static final String biomes = "biomes";
        public static final String blacklistDimensions = "blacklistDimensions";
        public static final String dimensions = "dimensions";
        public static final String height = "height";
        public static final String floor = "floor";
        public static final String ceiling = "ceiling";
        public static final String region = "region";
        public static final String noise = "noise";

        private Keys() {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static ConditionSettings from(JsonObject json, ConditionSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static ConditionSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    /**
     * Copies any values from the json into the builder, if present. The four nested noise blocks
     * are merged against whatever the builder already holds, so an inherited generator can be
     * partially overridden rather than replaced wholesale.
     */
    private static ConditionSettings copyInto(JsonObject json, Builder builder) {
        return new HjsonMapper(json)
            .mapBool(Keys.blacklistBiomes, builder::blacklistBiomes)
            .mapBiomes(Keys.biomes, builder::biomes)
            .mapBool(Keys.blacklistDimensions, builder::blacklistDimensions)
            .mapIntList(Keys.dimensions, builder::dimensions)
            .mapRange(Keys.height, builder::height)
            .mapObject(Keys.floor, o -> builder.floor(Optional.of(builder.floor
                .map(cfg -> NoiseMapSettings.from(o, cfg))
                .orElseGet(() -> NoiseMapSettings.from(o)))))
            .mapObject(Keys.ceiling, o -> builder.ceiling(Optional.of(builder.ceiling
                .map(cfg -> NoiseMapSettings.from(o, cfg))
                .orElseGet(() -> NoiseMapSettings.from(o)))))
            .mapObject(Keys.region, o -> builder.region(Optional.of(builder.region
                .map(cfg -> NoiseRegionSettings.from(o, cfg))
                .orElseGet(() -> NoiseRegionSettings.from(o)))))
            .mapObject(Keys.noise, o -> builder.noise(Optional.of(builder.noise
                .map(cfg -> NoiseSettings.from(o, cfg))
                .orElseGet(() -> NoiseSettings.from(o)))))
            .release(builder::build);
    }

    public static final class Builder {
        private boolean blacklistBiomes = false;
        private List<Biome> biomes = List.of();
        private boolean blacklistDimensions = false;
        private List<Integer> dimensions = List.of();
        private Range height = Range.of(0, 255);
        Optional<NoiseMapSettings> floor = Optional.empty();
        Optional<NoiseMapSettings> ceiling = Optional.empty();
        Optional<NoiseRegionSettings> region = Optional.empty();
        Optional<NoiseSettings> noise = Optional.empty();

        private Builder() {}

        private Builder(ConditionSettings source) {
            this.blacklistBiomes = source.blacklistBiomes;
            this.biomes = source.biomes;
            this.blacklistDimensions = source.blacklistDimensions;
            this.dimensions = source.dimensions;
            this.height = source.height;
            this.floor = source.floor;
            this.ceiling = source.ceiling;
            this.region = source.region;
            this.noise = source.noise;
        }

        public Builder blacklistBiomes(boolean blacklistBiomes) {
            this.blacklistBiomes = blacklistBiomes;
            return this;
        }

        public Builder biomes(List<Biome> biomes) {
            this.biomes = biomes;
            return this;
        }

        public Builder blacklistDimensions(boolean blacklistDimensions) {
            this.blacklistDimensions = blacklistDimensions;
            return this;
        }

        public Builder dimensions(List<Integer> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder height(Range height) {
            this.height = height;
            return this;
        }

        public Builder floor(Optional<NoiseMapSettings> floor) {
            this.floor = floor;
            return this;
        }

        public Builder ceiling(Optional<NoiseMapSettings> ceiling) {
            this.ceiling = ceiling;
            return this;
        }

        public Builder region(Optional<NoiseRegionSettings> region) {
            this.region = region;
            return this;
        }

        public Builder noise(Optional<NoiseSettings> noise) {
            this.noise = noise;
            return this;
        }

        public ConditionSettings build() {
            return new ConditionSettings(this.blacklistBiomes, this.biomes,
                this.blacklistDimensions, this.dimensions, this.height, this.floor, this.ceiling,
                this.region, this.noise);
        }
    }
}
