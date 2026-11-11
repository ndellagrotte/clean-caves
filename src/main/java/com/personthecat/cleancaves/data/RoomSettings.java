package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.util.HjsonMapper;
import org.hjson.JsonObject;

import static com.personthecat.cleancaves.util.CommonMethods.invert;

/** How a wider chamber should spawn at the origin of a tunnel system. */
public record RoomSettings(DecoratorSettings decorators, float scale, float stretch, int chance) {

    /** Default decorator settings for all room generators. */
    private static final DecoratorSettings DEFAULT_DECORATORS = DecoratorSettings.DEFAULTS;

    public static Builder builder() {
        return new Builder();
    }

    public static RoomSettings from(JsonObject json, OverrideSettings overrides) {
        final DecoratorSettings decorators = overrides.apply(DEFAULT_DECORATORS.toBuilder()).build();
        return copyInto(json, builder().decorators(decorators));
    }

    public static RoomSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static RoomSettings copyInto(JsonObject json, Builder builder) {
        final DecoratorSettings inherited = builder.decorators;
        return new HjsonMapper(json)
                .mapSelf(o -> builder.decorators(DecoratorSettings.from(o, inherited)))
                .mapFloat(Keys.SCALE, builder::scale)
                .mapFloat(Keys.STRETCH, builder::stretch)
                .mapFloat(Keys.CHANCE, f -> builder.chance(invert(f)))
                .release(builder::build);
    }

    /** JSON key names. */
    public static final class Keys {

        public static final String DECORATORS = "decorators";
        public static final String SCALE = "scale";
        public static final String STRETCH = "stretch";
        public static final String CHANCE = "chance";

        private Keys() { }

    }

    public static final class Builder {

        DecoratorSettings decorators = DEFAULT_DECORATORS;
        private float scale = 6.0F;
        private float stretch = 0.5F;
        private int chance = 10;

        private Builder() { }

        public Builder decorators(DecoratorSettings decorators) {
            this.decorators = decorators;
            return this;
        }

        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public Builder stretch(float stretch) {
            this.stretch = stretch;
            return this;
        }

        public Builder chance(int chance) {
            this.chance = chance;
            return this;
        }

        public RoomSettings build() {
            return new RoomSettings(this.decorators, this.scale, this.stretch, this.chance);
        }

    }

}
