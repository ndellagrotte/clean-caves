package com.personthecat.cleancaves.model;

import java.util.Random;

import static com.personthecat.cleancaves.util.CommonMethods.f;
import static com.personthecat.cleancaves.util.CommonMethods.numBetween;

public record FloatRange(float min, float max) {

    public float rand(Random rand) {
        return numBetween(rand, this.min, this.max);
    }

    public float diff() {
        return this.max - this.min;
    }

    @Override
    public String toString() {
        return f("Range[{}~{}]", this.min, this.max);
    }

}
