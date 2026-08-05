package com.personthecat.cleancaves.data;

import com.personthecat.cleancaves.util.XoRoShiRo;
import fastnoise.FastNoise;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.Random;

/**
 * Derives generator seeds. Shared verbatim by the three noise settings types, which each used to
 * carry their own identical copy.
 */
final class NoiseSeeds {

    private NoiseSeeds() {}

    /**
     * Generates a new seed from the configured base value, so that the same base produces the same
     * pattern in a given world. Without one, the world's RNG supplies an arbitrary seed.
     */
    static int derive(World world, Optional<Integer> seed) {
        return seed.map(num -> {
            final Random rand = new XoRoShiRo(world.getSeed());
            final FastNoise simple = new FastNoise(rand.nextInt());
            return Float.floatToIntBits(simple.GetNoise(num));
        }).orElseGet(world.rand::nextInt);
    }
}
