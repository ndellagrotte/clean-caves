package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import com.personthecat.cleancaves.util.HjsonTools;
import org.apache.commons.lang3.ArrayUtils;
import org.hjson.JsonObject;

import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Any settings that can be written at the top level of a preset to serve as default values.
 *
 * <p>Every field is optional: {@link #apply} pushes only the values the user actually wrote into a
 * feature's builder, leaving that feature's own defaults in place for everything else.
 */
public record OverrideSettings(Optional<Boolean> blacklistBiomes,
        Optional<List<Biome>> biomes,
        Optional<Boolean> blacklistDimensions,
        Optional<List<Integer>> dimensions,
        Optional<Range> height,
        Optional<NoiseMapSettings> floor,
        Optional<NoiseMapSettings> ceiling,
        Optional<NoiseRegionSettings> region,
        Optional<NoiseSettings> noise,
        Optional<List<IBlockState>> replaceableBlocks,
        Optional<Boolean> replaceDecorators,
        Optional<List<CaveBlockSettings>> caveBlocks,
        Optional<List<WallDecoratorSettings>> wallDecorators,
        Optional<List<PondSettings>> ponds,
        Optional<ShellSettings> shell,
        Optional<TunnelSettings> branches,
        Optional<RoomSettings> rooms,
        List<IBlockState> globalDecorators) {

    public static Builder builder() {
        return new Builder();
    }

    public static OverrideSettings from(JsonObject json) {
        final Builder builder = builder().globalDecorators(getAllDecorators(json));
        return new HjsonMapper(json)
                .mapBool(Keys.BLACKLIST_BIOMES, b -> builder.blacklistBiomes(Optional.of(b)))
                .mapBiomes(Keys.BIOMES, l -> builder.biomes(Optional.of(l)))
                .mapBool(Keys.BLACKLIST_DIMENSIONS, b -> builder.blacklistBiomes(Optional.of(b)))
                .mapIntList(Keys.DIMENSIONS, l -> builder.dimensions(Optional.of(l)))
                .mapRange(Keys.HEIGHT, r -> builder.height(Optional.of(r)))
                .mapObject(Keys.FLOOR, o -> builder.floor(Optional.of(NoiseMapSettings.from(o))))
                .mapObject(Keys.CEILING, o -> builder.ceiling(Optional.of(NoiseMapSettings.from(o))))
                .mapObject(Keys.NOISE, o -> builder.noise(Optional.of(NoiseSettings.from(o))))
                .mapStateList(Keys.REPLACEABLE_BLOCKS, l -> builder.replaceableBlocks(Optional.of(l)))
                .mapBool(Keys.REPLACE_DECORATORS, b -> builder.replaceDecorators(Optional.of(b)))
                .mapArray(Keys.CAVE_BLOCKS, CaveBlockSettings::from, l -> builder.caveBlocks(Optional.of(l)))
                .mapArray(Keys.WALL_DECORATORS, WallDecoratorSettings::from,
                        l -> builder.wallDecorators(Optional.of(l)))
                .mapArray(Keys.PONDS, PondSettings::from, l -> builder.ponds(Optional.of(l)))
                .mapObject(Keys.SHELL, s -> builder.shell(Optional.of(ShellSettings.from(s))))
                .mapObject(Keys.BRANCHES, b -> builder.branches(Optional.of(TunnelSettings.from(b))))
                .mapObject(Keys.ROOMS, r -> builder.rooms(Optional.of(RoomSettings.from(r))))
                .release(builder::build);
    }

    /**
     * Collects every block any decorator anywhere in the preset might place, so that carvers can
     * be told not to treat those blocks as untouched stone.
     */
    private static List<IBlockState> getAllDecorators(JsonObject json) {
        final List<IBlockState> decorators = new ArrayList<>();
        addAllDecorators(decorators, json); // Top-level overrides
        addAllDecorators(decorators, json, Keys.BRANCHES);
        addAllDecorators(decorators, json, Keys.ROOMS);
        addAllDecorators(decorators, json, CavePreset.Keys.TUNNELS);
        addAllDecorators(decorators, json, CavePreset.Keys.TUNNELS, TunnelSettings.Keys.BRANCHES);
        addAllDecorators(decorators, json, CavePreset.Keys.RAVINES);
        addAllDecorators(decorators, json, CavePreset.Keys.CAVERNS);
        addAll(decorators, json, ClusterSettings.Keys.STATES, CavePreset.Keys.CLUSTERS);
        addAll(decorators, json, LayerSettings.Keys.STATE, CavePreset.Keys.LAYERS);
        return decorators;
    }

    private static void addAllDecorators(List<IBlockState> decorators, JsonObject json, String... path) {
        HjsonTools.forEachAtPath(json, j -> addAll(decorators, j, WallDecoratorSettings.Keys.STATES),
                ArrayUtils.add(path, DecoratorSettings.Keys.WALL_DECORATORS));
        HjsonTools.forEachAtPath(json, j -> addAll(decorators, j, CaveBlockSettings.Keys.STATES),
                ArrayUtils.add(path, DecoratorSettings.Keys.CAVE_BLOCKS));
        HjsonTools.forEachAtPath(json, j -> addAll(decorators, j, PondSettings.Keys.STATES),
                ArrayUtils.add(path, DecoratorSettings.Keys.PONDS));
        HjsonTools.forEachAtPath(json, j -> addAll(decorators, j, ShellSettings.Decorator.Keys.STATES),
                ArrayUtils.addAll(path, DecoratorSettings.Keys.SHELL, ShellSettings.Keys.DECORATORS));
    }

    private static void addAll(List<IBlockState> decorators, JsonObject json, String field, String... path) {
        HjsonTools.forEachAtPath(json, j -> HjsonTools.getStateList(j, field).ifPresent(decorators::addAll), path);
    }

    public ConditionSettings.Builder apply(ConditionSettings.Builder builder) {
        this.blacklistBiomes.ifPresent(builder::blacklistBiomes);
        this.biomes.ifPresent(builder::biomes);
        this.blacklistDimensions.ifPresent(builder::blacklistDimensions);
        this.dimensions.ifPresent(builder::dimensions);
        this.height.ifPresent(builder::height);
        this.floor.ifPresent(n -> builder.floor(Optional.of(n)));
        this.ceiling.ifPresent(n -> builder.ceiling(Optional.of(n)));
        this.region.ifPresent(n -> builder.region(Optional.of(n)));
        this.noise.ifPresent(n -> builder.noise(Optional.of(n)));
        return builder;
    }

    public DecoratorSettings.Builder apply(DecoratorSettings.Builder builder) {
        builder.globalDecorators(this.globalDecorators);
        this.replaceableBlocks.ifPresent(builder::replaceableBlocks);
        this.replaceDecorators.ifPresent(builder::replaceDecorators);
        this.caveBlocks.ifPresent(builder::caveBlocks);
        this.wallDecorators.ifPresent(builder::wallDecorators);
        this.ponds.ifPresent(builder::ponds);
        this.shell.ifPresent(builder::shell);
        return builder;
    }

    public TunnelSettings.Builder apply(TunnelSettings.Builder builder) {
        this.branches.ifPresent(b -> builder.branches(Optional.of(b)));
        this.rooms.ifPresent(r -> builder.rooms(Optional.of(r)));
        return builder;
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
        public static final String REPLACEABLE_BLOCKS = "replaceableBlocks";
        public static final String REPLACE_DECORATORS = "replaceDecorators";
        public static final String CAVE_BLOCKS = "caveBlocks";
        public static final String WALL_DECORATORS = "wallDecorators";
        public static final String PONDS = "ponds";
        public static final String SHELL = "shell";
        public static final String BRANCHES = "branches";
        public static final String ROOMS = "rooms";
        public static final String GLOBAL_DECORATORS = "globalDecorators";

        private Keys() { }

    }

    public static final class Builder {

        private Optional<Boolean> blacklistBiomes = Optional.empty();
        private Optional<List<Biome>> biomes = Optional.empty();
        private Optional<Boolean> blacklistDimensions = Optional.empty();
        private Optional<List<Integer>> dimensions = Optional.empty();
        private Optional<Range> height = Optional.empty();
        private Optional<NoiseMapSettings> floor = Optional.empty();
        private Optional<NoiseMapSettings> ceiling = Optional.empty();
        private Optional<NoiseRegionSettings> region = Optional.empty();
        private Optional<NoiseSettings> noise = Optional.empty();
        private Optional<List<IBlockState>> replaceableBlocks = Optional.empty();
        private Optional<Boolean> replaceDecorators = Optional.empty();
        private Optional<List<CaveBlockSettings>> caveBlocks = Optional.empty();
        private Optional<List<WallDecoratorSettings>> wallDecorators = Optional.empty();
        private Optional<List<PondSettings>> ponds = Optional.empty();
        private Optional<ShellSettings> shell = Optional.empty();
        private Optional<TunnelSettings> branches = Optional.empty();
        private Optional<RoomSettings> rooms = Optional.empty();
        private List<IBlockState> globalDecorators = List.of();

        private Builder() { }

        public Builder blacklistBiomes(Optional<Boolean> blacklistBiomes) {
            this.blacklistBiomes = blacklistBiomes;
            return this;
        }

        public Builder biomes(Optional<List<Biome>> biomes) {
            this.biomes = biomes;
            return this;
        }

        public Builder blacklistDimensions(Optional<Boolean> blacklistDimensions) {
            this.blacklistDimensions = blacklistDimensions;
            return this;
        }

        public Builder dimensions(Optional<List<Integer>> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder height(Optional<Range> height) {
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

        public Builder replaceableBlocks(Optional<List<IBlockState>> replaceableBlocks) {
            this.replaceableBlocks = replaceableBlocks;
            return this;
        }

        public Builder replaceDecorators(Optional<Boolean> replaceDecorators) {
            this.replaceDecorators = replaceDecorators;
            return this;
        }

        public Builder caveBlocks(Optional<List<CaveBlockSettings>> caveBlocks) {
            this.caveBlocks = caveBlocks;
            return this;
        }

        public Builder wallDecorators(Optional<List<WallDecoratorSettings>> wallDecorators) {
            this.wallDecorators = wallDecorators;
            return this;
        }

        public Builder ponds(Optional<List<PondSettings>> ponds) {
            this.ponds = ponds;
            return this;
        }

        public Builder shell(Optional<ShellSettings> shell) {
            this.shell = shell;
            return this;
        }

        public Builder branches(Optional<TunnelSettings> branches) {
            this.branches = branches;
            return this;
        }

        public Builder rooms(Optional<RoomSettings> rooms) {
            this.rooms = rooms;
            return this;
        }

        public Builder globalDecorators(List<IBlockState> globalDecorators) {
            this.globalDecorators = globalDecorators;
            return this;
        }

        public OverrideSettings build() {
            return new OverrideSettings(this.blacklistBiomes, this.biomes, this.blacklistDimensions, this.dimensions,
                    this.height, this.floor, this.ceiling, this.region, this.noise, this.replaceableBlocks,
                    this.replaceDecorators, this.caveBlocks, this.wallDecorators, this.ponds, this.shell, this.branches,
                    this.rooms, this.globalDecorators);
        }

    }

}
