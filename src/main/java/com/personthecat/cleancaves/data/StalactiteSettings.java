package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.config.CavePreset;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import net.minecraft.block.state.IBlockState;

import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;

/** Large stalactites and stalagmites hanging from ceilings or rising from floors. */
@ParametersAreNonnullByDefault
public record StalactiteSettings(ConditionSettings conditions,
        IBlockState state,
        Type type,
        Size size,
        double chance,
        Range length,
        int space,
        boolean symmetrical,
        List<IBlockState> matchers) {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = CavePreset.Keys.STALACTITES;

    /** Default spawn conditions for all stalactite generators. */
    private static final ConditionSettings DEFAULT_CONDITIONS = ConditionSettings.builder()
            .height(Range.of(11, 55))
            .build();

    /** The default noise settings to be optionally used for stalactites. */
    public static final NoiseRegionSettings DEFAULT_NOISE = NoiseRegionSettings.builder()
            .frequency(0.025f)
            .threshold(Range.of(-0.425F))
            .build();

    /** JSON key names. */
    public static final class Keys {

        public static final String CONDITIONS = "conditions";
        public static final String STATE = "state";
        public static final String TYPE = "type";
        public static final String SIZE = "size";
        public static final String CHANCE = "chance";
        public static final String LENGTH = "length";
        public static final String SPACE = "space";
        public static final String SYMMETRICAL = "symmetrical";
        public static final String MATCHERS = "matchers";

        private Keys() { }

    }

    public static Builder builder() {
        return new Builder();
    }

    public static StalactiteSettings from(JsonObject json, OverrideSettings overrides) {
        final ConditionSettings conditions = overrides.apply(DEFAULT_CONDITIONS.toBuilder()).build();
        return copyInto(json, builder().conditions(conditions));
    }

    public static StalactiteSettings from(JsonObject json) {
        return copyInto(json, builder().conditions(DEFAULT_CONDITIONS));
    }

    private static StalactiteSettings copyInto(JsonObject json, Builder builder) {
        return new HjsonMapper(json)
                .mapRequiredState(Keys.STATE, FEATURE_NAME, builder::state)
                .mapSelf(o -> builder.conditions(ConditionSettings.from(o, builder.conditions)))
                .mapEnum(Keys.TYPE, Type.class, builder::type)
                .mapEnum(Keys.SIZE, Size.class, builder::size)
                .mapFloat(Keys.CHANCE, builder::chance)
                .mapRange(Keys.LENGTH, builder::length)
                .mapInt(Keys.SPACE, builder::space)
                .mapBool(Keys.SYMMETRICAL, builder::symmetrical)
                .mapStateList(Keys.MATCHERS, builder::matchers)
                .release(builder::build);
    }

    public enum Type {

        STALAGMITE,
        STALACTITE,
        SPELEOTHEM

    }

    public enum Size {

        SMALL,
        MEDIUM,
        LARGE,
        GIANT

    }

    public static final class Builder {

        ConditionSettings conditions = DEFAULT_CONDITIONS;
        /** Required -- {@code mapRequiredState} throws if the preset omits it. */
        private IBlockState state;
        private Type type = Type.STALACTITE;
        private Size size = Size.MEDIUM;
        private double chance = 0.167F;
        private Range length = Range.of(3, 5);
        private int space = 3;
        private boolean symmetrical = true;
        private List<IBlockState> matchers = List.of();

        private Builder() { }

        public Builder conditions(ConditionSettings conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder state(IBlockState state) {
            this.state = state;
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder size(Size size) {
            this.size = size;
            return this;
        }

        public Builder chance(double chance) {
            this.chance = chance;
            return this;
        }

        public Builder length(Range length) {
            this.length = length;
            return this;
        }

        public Builder space(int space) {
            this.space = space;
            return this;
        }

        public Builder symmetrical(boolean symmetrical) {
            this.symmetrical = symmetrical;
            return this;
        }

        public Builder matchers(List<IBlockState> matchers) {
            this.matchers = matchers;
            return this;
        }

        public StalactiteSettings build() {
            return new StalactiteSettings(this.conditions, this.state, this.type, this.size, this.chance, this.length,
                    this.space, this.symmetrical, this.matchers);
        }

    }

}
