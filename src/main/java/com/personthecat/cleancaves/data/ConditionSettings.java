package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.Optional;

/** Everything that decides whether a feature may spawn at a given place. */
public record ConditionSettings(boolean blacklistBiomes,
        List<Biome> biomes,
        boolean blacklistDimensions,
        List<Integer> dimensions,
        Range height,
        Optional<NoiseMapSettings> floor,
        Optional<NoiseMapSettings> ceiling,
        Optional<NoiseRegionSettings> region,
        Optional<NoiseSettings> noise) {

    public static Builder builder() {
        return new Builder();
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
                .mapBool(Keys.BLACKLIST_BIOMES, builder::blacklistBiomes)
                .mapBiomes(Keys.BIOMES, builder::biomes)
                .mapBool(Keys.BLACKLIST_DIMENSIONS, builder::blacklistDimensions)
                .mapIntList(Keys.DIMENSIONS, builder::dimensions)
                .mapRange(Keys.HEIGHT, builder::height)
                .mapObject(Keys.FLOOR,
                        o -> builder.floor(Optional.of(builder.floor
                                        .map(cfg -> NoiseMapSettings.from(o, cfg))
                                        .orElseGet(() -> NoiseMapSettings.from(o)))))
                .mapObject(Keys.CEILING,
                        o -> builder.ceiling(Optional.of(builder.ceiling
                                        .map(cfg -> NoiseMapSettings.from(o, cfg))
                                        .orElseGet(() -> NoiseMapSettings.from(o)))))
                .mapObject(Keys.REGION,
                        o -> builder.region(Optional.of(builder.region
                                        .map(cfg -> NoiseRegionSettings.from(o, cfg))
                                        .orElseGet(() -> NoiseRegionSettings.from(o)))))
                .mapObject(Keys.NOISE,
                        o -> builder.noise(Optional.of(builder.noise
                                        .map(cfg -> NoiseSettings.from(o, cfg))
                                        .orElseGet(() -> NoiseSettings.from(o)))))
                .release(builder::build);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    /** JSON key names. */
    public static final class Keys {

        public static final String BLACKLIST_BIOMES = "blacklistBiomes";
        public static final String BIOMES = "biomes";
        public static final String BLACKLIST_DIMENSIONS = "blacklistDimensions";
        public static final String DIMENSIONS = "dimensions";
        public static final String HEIGHT = "height";
        public static final String FLOOR = "floor";
        public static final String CEILING = "ceiling";
        public static final String REGION = "region";
        public static final String NOISE = "noise";

        private Keys() { }

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

        private Builder() { }

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
            return new ConditionSettings(this.blacklistBiomes, this.biomes, this.blacklistDimensions, this.dimensions,
                    this.height, this.floor, this.ceiling, this.region, this.noise);
        }

    }

}
