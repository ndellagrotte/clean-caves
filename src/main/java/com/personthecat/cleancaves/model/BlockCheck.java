package com.personthecat.cleancaves.model;

import com.personthecat.cleancaves.data.StructureSettings;
import com.personthecat.cleancaves.util.HjsonMapper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.List;

import static com.personthecat.cleancaves.util.CommonMethods.runExF;

/** A requirement that specific blocks be present at specific offsets before a structure spawns. */
public record BlockCheck(List<IBlockState> matchers, List<BlockPos> positions) {

    /** The name of this feature to be used globally in serialization. */
    private static final String FEATURE_NAME = StructureSettings.Keys.blockChecks;

    /** JSON key names. */
    public static final class Keys {
        public static final String matchers = "matchers";
        public static final String positions = "positions";

        private Keys() {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public static BlockCheck fromValue(JsonValue json) {
        if (json.isObject()) {
            return from(json.asObject());
        } else if (json.isArray()) {
            return from(json.asArray());
        }
        throw runExF("Expected object or array: {}", json);
    }

    public static BlockCheck from(JsonObject json) {
        final Builder builder = builder();
        return new HjsonMapper(json)
            .mapRequiredStateList(Keys.matchers, FEATURE_NAME, builder::matchers)
            .mapRequiredBlockPosList(Keys.positions, FEATURE_NAME, builder::positions)
            .release(builder::build);
    }

    public static BlockCheck from(JsonArray json) {
        final JsonArray matchers = new JsonArray();
        final JsonArray positions = new JsonArray();
        for (final JsonValue value : json) {
            if (value.isString()) {
                matchers.add(value);
            } else if (value.isArray()) {
                positions.add(value);
            } else {
                throw runExF("Expected array or string: {}", value);
            }
        }
        // This allows all other error messages and requirements to be consistent.
        return from(new JsonObject().add(Keys.matchers, matchers).add(Keys.positions, positions));
    }

    public static final class Builder {
        private List<IBlockState> matchers = List.of();
        private List<BlockPos> positions = List.of();

        private Builder() {}

        public Builder matchers(List<IBlockState> matchers) {
            this.matchers = matchers;
            return this;
        }

        public Builder positions(List<BlockPos> positions) {
            this.positions = positions;
            return this;
        }

        public BlockCheck build() {
            return new BlockCheck(this.matchers, this.positions);
        }
    }
}
