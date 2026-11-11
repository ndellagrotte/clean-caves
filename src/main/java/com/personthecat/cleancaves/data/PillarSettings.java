package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

import java.util.Optional;
import javax.annotation.ParametersAreNonnullByDefault;

import static com.personthecat.cleancaves.util.CommonMethods.runExF;

/** Giant pillars with optional corner blocks made from stairs. */
@ParametersAreNonnullByDefault
public record PillarSettings(ConditionSettings conditions,
        IBlockState state,
        int count,
        Range length,
        Optional<BlockStairs> stairBlock) {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Keys.PILLARS;

    /** Default spawn conditions for all pillar generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
            .height(Range.of(10, 50))
            .build();

    public static Builder builder() {
        return new Builder();
    }

    public static PillarSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static PillarSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static PillarSettings copyInto(JsonObject json, Builder builder) {
        final ConditionSettings inherited = builder.conditions;
        return new HjsonMapper(json)
                .mapRequiredState(Keys.STATE, FEATURE_NAME, builder::state)
                .mapSelf(o -> builder.conditions(ConditionSettings.from(o, inherited)))
                .mapInt(Keys.COUNT, builder::count)
                .mapRange(Keys.LENGTH, builder::length)
                .mapState(Keys.STAIR_BLOCK, s -> builder.stairBlock(Optional.of(toStairBlock(s))))
                .release(builder::build);
    }

    private static BlockStairs toStairBlock(IBlockState state) {
        final Block block = state.getBlock();
        if (block instanceof BlockStairs stairs) {
            return stairs;
        }
        throw runExF("Error: the input block, {}, is not a valid stair block.", block);
    }

    /** JSON key names. */
    public static final class Keys {

        public static final String CONDITIONS = "conditions";
        public static final String STATE = "state";
        public static final String COUNT = "count";
        public static final String LENGTH = "length";
        public static final String STAIR_BLOCK = "stairBlock";

        private Keys() { }

    }

    public static final class Builder {

        ConditionSettings conditions = DEFAULT_CONDITIONS;
        /** Required -- {@code mapRequiredState} throws if the preset omits it. */
        private IBlockState state;
        private int count = 15;
        private Range length = Range.of(5, 12);
        private Optional<BlockStairs> stairBlock = Optional.empty();

        private Builder() { }

        public Builder conditions(ConditionSettings conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder state(IBlockState state) {
            this.state = state;
            return this;
        }

        public Builder count(int count) {
            this.count = count;
            return this;
        }

        public Builder length(Range length) {
            this.length = length;
            return this;
        }

        public Builder stairBlock(Optional<BlockStairs> stairBlock) {
            this.stairBlock = stairBlock;
            return this;
        }

        public PillarSettings build() {
            return new PillarSettings(this.conditions, this.state, this.count, this.length, this.stairBlock);
        }

    }

}
