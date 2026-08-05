package com.personthecat.cleancaves.util;

/** There is no {@code IntTriPredicate} in the JDK, and boxing here would be measurable. */
@FunctionalInterface
public interface PositionPredicate {
    boolean test(int x, int y, int z);
}
