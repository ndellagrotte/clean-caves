package com.personthecat.cleancaves.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.message.ParameterizedMessage;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

/**
 * Helpers shared across the mod. Anything the JDK now provides directly -- {@code Optional}
 * shorthands, list mapping, linear searches -- has been dropped in favour of the standard API;
 * what remains is either Minecraft-specific or has no equivalent.
 */
public final class CommonMethods {

    private CommonMethods() {}

    /** Returns a clean-looking, general-purpose RuntimeException. */
    public static RuntimeException runEx(String x) {
        return new RuntimeException(x);
    }

    public static RuntimeException runEx(String x, Throwable t) {
        return new RuntimeException(x, t);
    }

    /** Shorthand for a RuntimeException using {@code {}} interpolation. */
    public static RuntimeException runExF(String x, Object... args) {
        return new RuntimeException(f(x, args));
    }

    /**
     * Interpolates {@code {}} placeholders, matching the syntax Log4j2 uses for log messages so
     * that the same template reads identically whether it is logged or thrown.
     */
    public static String f(String s, Object... args) {
        return ParameterizedMessage.format(s, args);
    }

    /** Determines the extension of the input file, or {@code ""} when it has none. */
    public static String extension(Path file) {
        return extension(file.getFileName().toString());
    }

    public static String extension(String filename) {
        final int index = filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index + 1);
    }

    /** Gets the file name, minus every extension. */
    public static String noExtension(Path file) {
        return noExtension(file.getFileName().toString());
    }

    /** Removes any extensions from the input filename. */
    public static String noExtension(String name) {
        final int index = name.indexOf('.');
        return index < 0 ? name : name.substring(0, index);
    }

    /** Returns a random number between the input bounds, inclusive. */
    public static int numBetween(Random rand, int min, int max) {
        return min == max ? min : rand.nextInt(max - min + 1) + min;
    }

    /** Returns a random number between the input bounds, inclusive. */
    public static float numBetween(Random rand, float min, float max) {
        return min == max ? min : rand.nextFloat() * (max - min) + min;
    }

    /** Variant of {@link Arrays#sort} which returns the array. */
    public static int[] sort(int[] array) {
        Arrays.sort(array);
        return array;
    }

    public static float[] sort(float[] array) {
        Arrays.sort(array);
        return array;
    }

    /** Divides 1 / {@code value} without any divide by zero errors or unsightly casting. */
    public static int invert(double value) {
        return value == 0 ? Integer.MAX_VALUE : (int) (1 / value);
    }

    /** Retrieves a biome from either a registry name or a unique ID. */
    public static Optional<Biome> getBiome(String biomeName) {
        return Optional.ofNullable(ForgeRegistries.BIOMES.getValue(new ResourceLocation(biomeName)));
    }

    public static Optional<Biome> getBiome(int biomeNumber) {
        return Optional.ofNullable(Biome.getBiomeForId(biomeNumber));
    }

    public static Biome[] getBiomes(Type biomeType) {
        return BiomeDictionary.getBiomes(biomeType).toArray(new Biome[0]);
    }

    public static Type getBiomeType(String name) {
        return Type.getType(name);
    }

    /**
     * Variant of {@code ForgeRegistries.BLOCKS#getValue} that does not substitute air for blocks
     * that aren't found. Accepts either {@code mod:block} or {@code mod:block:meta}.
     */
    public static Optional<IBlockState> getBlockState(String registryName) {
        // Ensure that air is returned if that is the query.
        if (registryName.equals("air") || registryName.equals("minecraft:air")) {
            return Optional.of(Blocks.AIR.getDefaultState());
        }

        final String[] split = registryName.split(":");
        if (split.length == 0 || split.length > 3) {
            throw runExF("Syntax error: could not determine block state from {}", registryName);
        }

        // If the end of the string is numeric, it must be the metadata.
        final String end = split[split.length - 1];
        if (StringUtils.isNumeric(end)) {
            final int meta = Integer.parseInt(end);
            return getBlock(registryName.replace(":" + end, ""), meta);
        }
        return getBlock(registryName, 0);
    }

    /**
     * Internal variant of the above that never returns air, ensuring a valid block has been
     * determined -- except of course in cases where that block is air.
     */
    @SuppressWarnings("deprecation") // No good alternative in 1.12. Just ignoring this.
    private static Optional<IBlockState> getBlock(String registryName, int meta) {
        final Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(registryName));
        if (block == null || Blocks.AIR.equals(block)) {
            return Optional.empty();
        }
        return Optional.of(block.getStateFromMeta(meta));
    }
}
