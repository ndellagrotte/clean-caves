package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;

import java.util.List;
import java.util.Optional;

/** A band of stone spawned across a height range, following a noise-driven ceiling. */
public record LayerSettings(
    List<IBlockState> matchers,
    ConditionSettings conditions,
    IBlockState state
) {
    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Keys.layers;

    /** The default noise values used for all layers. */
    private static final NoiseMapSettings DEFAULT_NOISE = NoiseMapSettings.builder()
        .frequency(0.015f).range(Range.of(-7, 7)).build();

    /** Default spawn conditions for all layer generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
        .height(Range.of(0, 20)).ceiling(Optional.of(DEFAULT_NOISE)).build();

    /** JSON key names. */
    public static final class Keys {
        public static final String matchers = "matchers";
        public static final String conditions = "conditions";
        public static final String state = "state";

        private Keys() {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LayerSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static LayerSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static LayerSettings copyInto(JsonObject json, Builder builder) {
        final ConditionSettings inherited = builder.conditions;
        return new HjsonMapper(json)
            .mapRequiredState(Keys.state, FEATURE_NAME, builder::state)
            .mapSelf(o -> builder.conditions(ConditionSettings.from(o, inherited)))
            .mapStateList(Keys.matchers, builder::matchers)
            .release(builder::build);
    }

    public static final class Builder {
        private List<IBlockState> matchers = List.of(Blocks.STONE.getDefaultState());
        ConditionSettings conditions = DEFAULT_CONDITIONS;
        /** Required -- {@code mapRequiredState} throws if the preset omits it. */
        private IBlockState state;

        private Builder() {}

        public Builder matchers(List<IBlockState> matchers) {
            this.matchers = matchers;
            return this;
        }

        public Builder conditions(ConditionSettings conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder state(IBlockState state) {
            this.state = state;
            return this;
        }

        public LayerSettings build() {
            return new LayerSettings(this.matchers, this.conditions, this.state);
        }
    }
}
