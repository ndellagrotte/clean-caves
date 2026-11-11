package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.BlockCheck;
import com.personthecat.cleancaves.model.Direction;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.template.PlacementSettings;

import java.util.List;

/** An NBT structure template placed against a cave surface. */
public record StructureSettings(ConditionSettings conditions,
        String name,
        PlacementSettings placement,
        List<IBlockState> matchers,
        Direction.Container directions,
        List<BlockPos> airChecks,
        List<BlockPos> solidChecks,
        List<BlockPos> nonSolidChecks,
        List<BlockPos> waterChecks,
        List<BlockCheck> blockChecks,
        boolean checkSurface,
        BlockPos offset,
        float chance,
        int count,
        boolean debugSpawns,
        String command,
        boolean rotateRandomly) {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Keys.STRUCTURES;

    /** Default spawn conditions for all structure generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
            .height(Range.of(10, 50))
            .build();

    public static Builder builder() {
        return new Builder();
    }

    public static StructureSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static StructureSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static StructureSettings copyInto(JsonObject json, Builder builder) {
        final ConditionSettings inherited = builder.conditions;
        return new HjsonMapper(json)
                .mapRequiredString(Keys.NAME, FEATURE_NAME, builder::name)
                .mapSelf(o -> builder.conditions(ConditionSettings.from(o, inherited)))
                .mapPlacementSettings(builder::placement)
                .mapStateList(Keys.MATCHERS, builder::matchers)
                .mapDirectionList(Keys.DIRECTIONS, l -> builder.directions(Direction.Container.from(l)))
                .mapBlockPosList(Keys.AIR_CHECKS, builder::airChecks)
                .mapBlockPosList(Keys.SOLID_CHECKS, builder::solidChecks)
                .mapBlockPosList(Keys.NON_SOLID_CHECKS, builder::nonSolidChecks)
                .mapBlockPosList(Keys.WATER_CHECKS, builder::waterChecks)
                .mapValueArray(Keys.BLOCK_CHECKS, BlockCheck::fromValue, builder::blockChecks)
                .mapBool(Keys.CHECK_SURFACE, builder::checkSurface)
                .mapBlockPos(Keys.OFFSET, builder::offset)
                .mapFloat(Keys.CHANCE, builder::chance)
                .mapInt(Keys.COUNT, builder::count)
                .mapBool(Keys.DEBUG_SPAWNS, builder::debugSpawns)
                .mapString(Keys.COMMAND, builder::command)
                .mapBool(Keys.ROTATE_RANDOMLY, builder::rotateRandomly)
                .release(builder::build);
    }

    /** JSON key names. */
    public static final class Keys {

        public static final String CONDITIONS = "conditions";
        public static final String NAME = "name";
        public static final String PLACEMENT = "placement";
        public static final String MATCHERS = "matchers";
        public static final String DIRECTIONS = "directions";
        public static final String AIR_CHECKS = "airChecks";
        public static final String SOLID_CHECKS = "solidChecks";
        public static final String NON_SOLID_CHECKS = "nonSolidChecks";
        public static final String WATER_CHECKS = "waterChecks";
        public static final String BLOCK_CHECKS = "blockChecks";
        public static final String CHECK_SURFACE = "checkSurface";
        public static final String OFFSET = "offset";
        public static final String CHANCE = "chance";
        public static final String COUNT = "count";
        public static final String DEBUG_SPAWNS = "debugSpawns";
        public static final String COMMAND = "command";
        public static final String ROTATE_RANDOMLY = "rotateRandomly";

        private Keys() { }

    }

    public static final class Builder {

        ConditionSettings conditions = DEFAULT_CONDITIONS;
        /** Required -- {@code mapRequiredString} throws if the preset omits it. */
        private String name;
        private PlacementSettings placement = new PlacementSettings().setReplacedBlock(Blocks.STONE);
        private List<IBlockState> matchers = List.of(Blocks.STONE.getDefaultState());
        private Direction.Container directions = Direction.Container.all();
        private List<BlockPos> airChecks = List.of();
        private List<BlockPos> solidChecks = List.of();
        private List<BlockPos> nonSolidChecks = List.of();
        private List<BlockPos> waterChecks = List.of();
        private List<BlockCheck> blockChecks = List.of();
        private boolean checkSurface = true;
        private BlockPos offset = BlockPos.ORIGIN;
        private float chance = 1.0F;
        private int count = 1;
        private boolean debugSpawns = false;
        private String command = "";
        private boolean rotateRandomly = false;

        private Builder() { }

        public Builder conditions(ConditionSettings conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder placement(PlacementSettings placement) {
            this.placement = placement;
            return this;
        }

        public Builder matchers(List<IBlockState> matchers) {
            this.matchers = matchers;
            return this;
        }

        public Builder directions(Direction.Container directions) {
            this.directions = directions;
            return this;
        }

        public Builder airChecks(List<BlockPos> airChecks) {
            this.airChecks = airChecks;
            return this;
        }

        public Builder solidChecks(List<BlockPos> solidChecks) {
            this.solidChecks = solidChecks;
            return this;
        }

        public Builder nonSolidChecks(List<BlockPos> nonSolidChecks) {
            this.nonSolidChecks = nonSolidChecks;
            return this;
        }

        public Builder waterChecks(List<BlockPos> waterChecks) {
            this.waterChecks = waterChecks;
            return this;
        }

        public Builder blockChecks(List<BlockCheck> blockChecks) {
            this.blockChecks = blockChecks;
            return this;
        }

        public Builder checkSurface(boolean checkSurface) {
            this.checkSurface = checkSurface;
            return this;
        }

        public Builder offset(BlockPos offset) {
            this.offset = offset;
            return this;
        }

        public Builder chance(float chance) {
            this.chance = chance;
            return this;
        }

        public Builder count(int count) {
            this.count = count;
            return this;
        }

        public Builder debugSpawns(boolean debugSpawns) {
            this.debugSpawns = debugSpawns;
            return this;
        }

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder rotateRandomly(boolean rotateRandomly) {
            this.rotateRandomly = rotateRandomly;
            return this;
        }

        public StructureSettings build() {
            return new StructureSettings(this.conditions, this.name, this.placement, this.matchers, this.directions,
                    this.airChecks, this.solidChecks, this.nonSolidChecks, this.waterChecks, this.blockChecks,
                    this.checkSurface, this.offset, this.chance, this.count, this.debugSpawns, this.command,
                    this.rotateRandomly);
        }

    }

}
