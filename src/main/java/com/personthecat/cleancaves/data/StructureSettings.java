package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.BlockCheck;
import com.personthecat.cleancaves.model.Direction;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import org.hjson.JsonObject;

import java.util.List;

/** An NBT structure template placed against a cave surface. */
public record StructureSettings(
    ConditionSettings conditions,
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
    boolean rotateRandomly
) {
    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Keys.structures;

    /** Default spawn conditions for all structure generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(10, 50)).build();

    /** JSON key names. */
    public static final class Keys {
        public static final String conditions = "conditions";
        public static final String name = "name";
        public static final String placement = "placement";
        public static final String matchers = "matchers";
        public static final String directions = "directions";
        public static final String airChecks = "airChecks";
        public static final String solidChecks = "solidChecks";
        public static final String nonSolidChecks = "nonSolidChecks";
        public static final String waterChecks = "waterChecks";
        public static final String blockChecks = "blockChecks";
        public static final String checkSurface = "checkSurface";
        public static final String offset = "offset";
        public static final String chance = "chance";
        public static final String count = "count";
        public static final String debugSpawns = "debugSpawns";
        public static final String command = "command";
        public static final String rotateRandomly = "rotateRandomly";

        private Keys() {}
    }

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
            .mapRequiredString(Keys.name, FEATURE_NAME, builder::name)
            .mapSelf(o -> builder.conditions(ConditionSettings.from(o, inherited)))
            .mapPlacementSettings(builder::placement)
            .mapStateList(Keys.matchers, builder::matchers)
            .mapDirectionList(Keys.directions, l -> builder.directions(Direction.Container.from(l)))
            .mapBlockPosList(Keys.airChecks, builder::airChecks)
            .mapBlockPosList(Keys.solidChecks, builder::solidChecks)
            .mapBlockPosList(Keys.nonSolidChecks, builder::nonSolidChecks)
            .mapBlockPosList(Keys.waterChecks, builder::waterChecks)
            .mapValueArray(Keys.blockChecks, BlockCheck::fromValue, builder::blockChecks)
            .mapBool(Keys.checkSurface, builder::checkSurface)
            .mapBlockPos(Keys.offset, builder::offset)
            .mapFloat(Keys.chance, builder::chance)
            .mapInt(Keys.count, builder::count)
            .mapBool(Keys.debugSpawns, builder::debugSpawns)
            .mapString(Keys.command, builder::command)
            .mapBool(Keys.rotateRandomly, builder::rotateRandomly)
            .release(builder::build);
    }

    public static final class Builder {
        ConditionSettings conditions = DEFAULT_CONDITIONS;
        /** Required -- {@code mapRequiredString} throws if the preset omits it. */
        private String name;
        private PlacementSettings placement = new PlacementSettings().setReplacedBlock(Blocks.STONE);
        private List<IBlockState> matchers = List.of(Blocks.STONE.getDefaultState());
        private Direction.Container directions = Direction.Container.none();
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

        private Builder() {}

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
            return new StructureSettings(this.conditions, this.name, this.placement, this.matchers,
                this.directions, this.airChecks, this.solidChecks, this.nonSolidChecks,
                this.waterChecks, this.blockChecks, this.checkSurface, this.offset, this.chance,
                this.count, this.debugSpawns, this.command, this.rotateRandomly);
        }
    }
}
