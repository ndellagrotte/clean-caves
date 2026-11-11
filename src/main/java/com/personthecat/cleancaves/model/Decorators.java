package com.personthecat.cleancaves.model;

import com.personthecat.cleancaves.data.DecoratorSettings;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * {@link DecoratorSettings} compiled against a specific world -- noise generators materialized and
 * the replaceable-block test reduced to a single predicate.
 */
public final class Decorators {

    /** Determines whether this carver can replace any given block. */
    public final Predicate<IBlockState> canReplace;

    /** Determines which block to place at the given coordinates. */
    public final List<ConfiguredCaveBlock> caveBlocks;

    /** Determines which blocks to place to the side of the given coordinates. */
    public final WallDecoratorMap wallMap;

    /** Determines which blocks to place in the shape of a pond below this feature. */
    public final List<ConfiguredPond> ponds;

    /** Determines which blocks to place surrounding the current feature. */
    public final ConfiguredShell shell;

    private Decorators(DecoratorSettings settings, World world) {
        this.canReplace = compileCanReplace(settings);
        this.caveBlocks = settings.caveBlocks().stream().map(b -> new ConfiguredCaveBlock(b, world)).toList();
        this.wallMap = WallDecoratorMap.sort(settings.wallDecorators(), world);
        this.ponds = settings.ponds().stream().map(p -> new ConfiguredPond(p, world)).toList();
        this.shell = new ConfiguredShell(settings.shell(), world);
    }

    public static Decorators compile(DecoratorSettings settings, World world) {
        return new Decorators(settings, world);
    }

    // This would ideally be adapted to check for other *kinds* of block features.
    private static Predicate<IBlockState> compileCanReplace(DecoratorSettings settings) {
        final Set<IBlockState> replaceable = new HashSet<>(settings.replaceableBlocks());
        if (replaceable.isEmpty()) {
            return s -> !s.getBlock().equals(Blocks.BEDROCK);
        } else if (settings.replaceSolidBlocks()) {
            return s -> s.getMaterial().isSolid() && !s.getBlock().equals(Blocks.BEDROCK);
        } else if (settings.replaceDecorators()) {
            replaceable.addAll(settings.globalDecorators());
            settings.caveBlocks().forEach(c -> replaceable.addAll(c.states()));
            settings.wallDecorators().forEach(w -> replaceable.addAll(w.states()));
            settings.ponds().forEach(p -> replaceable.addAll(p.states()));
            settings.shell().decorators().forEach(s -> replaceable.addAll(s.states()));
        }
        if (replaceable.size() == 1) {
            final IBlockState state = replaceable.iterator().next();
            return state::equals;
        }
        return replaceable::contains;
    }

}
