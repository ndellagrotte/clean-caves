package com.personthecat.cleancaves.util;

/**
 * A set of y coordinates in a chunk column, stored as four words. Column passes build these on
 * helper threads and the main thread then walks them in y order without touching noise again.
 */
public final class BlockMask {

    /** A chunk is 256 blocks tall, so a mask is the only state a column needs. */
    private static final int WORDS = 4;

    private final long[] words = new long[WORDS];

    /**
     * Marks {@code y}. Callers are expected to stay inside {@code [0, 256)}.
     *
     * @param y The y coordinate to mark.
     */
    public void set(int y) {
        this.words[y >> 6] |= 1L << y;
    }

    public boolean get(int y) {
        return (this.words[y >> 6] & (1L << y)) != 0L;
    }

    /** Whether no coordinate is marked. */
    public boolean isEmpty() {
        for (int i = 0; i < WORDS; i++) {
            if (this.words[i] != 0L) {
                return false;
            }
        }
        return true;
    }

    public void clear() {
        for (int i = 0; i < WORDS; i++) {
            this.words[i] = 0L;
        }
    }

}
