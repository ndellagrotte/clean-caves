package com.personthecat.cleancaves.util;

/** There is no {@code IntTriConsumer} in the JDK, and boxing here would be measurable. */
@FunctionalInterface
public interface PositionConsumer {

    void accept(int x, int y, int z);

}
