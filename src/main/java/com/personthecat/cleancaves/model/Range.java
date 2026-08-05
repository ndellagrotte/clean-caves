package com.personthecat.cleancaves.model;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import static com.personthecat.cleancaves.util.CommonMethods.f;
import static com.personthecat.cleancaves.util.CommonMethods.numBetween;

/**
 * An inclusive-minimum, exclusive-maximum integer range.
 *
 * <p>Sealed rather than a plain record because the empty range is genuinely distinct: it reports
 * {@link #isEmpty()} even though a {@code Bounded(0, 0)} would produce the same values for
 * everything else.
 */
public sealed interface Range extends Iterable<Integer> {

    int min();

    int max();

    int rand(Random rand);

    boolean contains(int num);

    int diff();

    boolean isEmpty();

    static Range of(int a, int b) {
        return a > b ? new Bounded(b, a) : new Bounded(a, b);
    }

    static Range of(int max) {
        return new Bounded(max, max);
    }

    static FloatRange of(float a, float b) {
        return a > b ? new FloatRange(b, a) : new FloatRange(a, b);
    }

    static FloatRange of(float a) {
        return new FloatRange(a, a);
    }

    /** Produces the empty range when the bounds do not describe a usable span. */
    static Range checkedOrEmpty(int min, int max) {
        return max > min ? new Bounded(min, max) : Empty.INSTANCE;
    }

    static Range empty() {
        return Empty.INSTANCE;
    }

    record Bounded(int min, int max) implements Range {

        @Override
        public int rand(Random rand) {
            return numBetween(rand, this.min, this.max);
        }

        @Override
        public boolean contains(int num) {
            return num >= this.min && num < this.max;
        }

        @Override
        public int diff() {
            return this.max - this.min;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Nonnull
        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<>() {
                int i = Bounded.this.min;

                @Override
                public boolean hasNext() {
                    return this.i < Bounded.this.max;
                }

                @Override
                public Integer next() {
                    return this.i++;
                }
            };
        }

        @Override
        public String toString() {
            return f("Range[{}~{}]", this.min, this.max);
        }
    }

    record Empty() implements Range {

        static final Empty INSTANCE = new Empty();

        @Override
        public int min() {
            return 0;
        }

        @Override
        public int max() {
            return 0;
        }

        @Override
        public int rand(Random rand) {
            return 0;
        }

        @Override
        public boolean contains(int num) {
            return false;
        }

        @Override
        public int diff() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Nonnull
        @Override
        public Iterator<Integer> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public String toString() {
            return "Range[empty]";
        }
    }
}
