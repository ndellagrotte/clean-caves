package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.util.HjsonMapper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import org.hjson.JsonObject;

import java.util.List;

/** Everything a world carver places on or around the blocks it removes. */
public record DecoratorSettings(
    List<IBlockState> replaceableBlocks,
    boolean replaceDecorators,
    boolean replaceSolidBlocks,
    List<CaveBlockSettings> caveBlocks,
    List<WallDecoratorSettings> wallDecorators,
    List<PondSettings> ponds,
    ShellSettings shell,
    List<IBlockState> globalDecorators
) {
    /** Common default settings to be used by all world carvers. */
    public static final DecoratorSettings DEFAULTS = builder().build();

    /** JSON key names. */
    public static final class Keys {
        public static final String replaceableBlocks = "replaceableBlocks";
        public static final String replaceDecorators = "replaceDecorators";
        public static final String replaceSolidBlocks = "replaceSolidBlocks";
        public static final String caveBlocks = "caveBlocks";
        public static final String wallDecorators = "wallDecorators";
        public static final String ponds = "ponds";
        public static final String shell = "shell";
        public static final String globalDecorators = "globalDecorators";

        private Keys() {}
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static DecoratorSettings from(JsonObject json, DecoratorSettings defaults) {
        return copyInto(json, defaults.toBuilder());
    }

    public static DecoratorSettings from(JsonObject json) {
        return copyInto(json, builder());
    }

    private static DecoratorSettings copyInto(JsonObject json, Builder builder) {
        return new HjsonMapper(json)
            .mapStateList(Keys.replaceableBlocks, builder::replaceableBlocks)
            .mapBool(Keys.replaceDecorators, builder::replaceDecorators)
            .mapBool(Keys.replaceSolidBlocks, builder::replaceSolidBlocks)
            .mapArray(Keys.caveBlocks, CaveBlockSettings::from, builder::caveBlocks)
            .mapArray(Keys.wallDecorators, WallDecoratorSettings::from, builder::wallDecorators)
            .mapArray(Keys.ponds, PondSettings::from, builder::ponds)
            .mapObject(Keys.shell, o -> builder.shell(ShellSettings.from(o)))
            .release(builder::build);
    }

    public static final class Builder {
        private List<IBlockState> replaceableBlocks = List.of(
            Blocks.STONE.getDefaultState(), Blocks.DIRT.getDefaultState(), Blocks.GRASS.getDefaultState());
        private boolean replaceDecorators = true;
        private boolean replaceSolidBlocks = false;
        private List<CaveBlockSettings> caveBlocks = List.of();
        private List<WallDecoratorSettings> wallDecorators = List.of();
        private List<PondSettings> ponds = List.of();
        private ShellSettings shell = ShellSettings.builder().build();
        private List<IBlockState> globalDecorators = List.of();

        private Builder() {}

        private Builder(DecoratorSettings source) {
            this.replaceableBlocks = source.replaceableBlocks;
            this.replaceDecorators = source.replaceDecorators;
            this.replaceSolidBlocks = source.replaceSolidBlocks;
            this.caveBlocks = source.caveBlocks;
            this.wallDecorators = source.wallDecorators;
            this.ponds = source.ponds;
            this.shell = source.shell;
            this.globalDecorators = source.globalDecorators;
        }

        public Builder replaceableBlocks(List<IBlockState> replaceableBlocks) {
            this.replaceableBlocks = replaceableBlocks;
            return this;
        }

        public Builder replaceDecorators(boolean replaceDecorators) {
            this.replaceDecorators = replaceDecorators;
            return this;
        }

        public Builder replaceSolidBlocks(boolean replaceSolidBlocks) {
            this.replaceSolidBlocks = replaceSolidBlocks;
            return this;
        }

        public Builder caveBlocks(List<CaveBlockSettings> caveBlocks) {
            this.caveBlocks = caveBlocks;
            return this;
        }

        public Builder wallDecorators(List<WallDecoratorSettings> wallDecorators) {
            this.wallDecorators = wallDecorators;
            return this;
        }

        public Builder ponds(List<PondSettings> ponds) {
            this.ponds = ponds;
            return this;
        }

        public Builder shell(ShellSettings shell) {
            this.shell = shell;
            return this;
        }

        public Builder globalDecorators(List<IBlockState> globalDecorators) {
            this.globalDecorators = globalDecorators;
            return this;
        }

        public DecoratorSettings build() {
            return new DecoratorSettings(this.replaceableBlocks, this.replaceDecorators,
                this.replaceSolidBlocks, this.caveBlocks, this.wallDecorators, this.ponds,
                this.shell, this.globalDecorators);
        }
    }
}
