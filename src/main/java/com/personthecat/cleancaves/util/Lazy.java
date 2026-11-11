package com.personthecat.cleancaves.util;

import java.util.function.Supplier;

/**
 * A memoizing {@link Supplier}. Used where a value cannot exist at construction time but the field
 * holding it should still be final.
 *
 * <p>Unlike the original, a supplier returning {@code null} is a legitimate -- and remembered --
 * result rather than a repeated recomputation ending in a {@code NullPointerException}.
 */
public final class Lazy<T> implements Supplier<T> {

    private Supplier<? extends T> supplier;
    private T value;

    private Lazy(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    private Lazy(T value) {
        this.value = value;
        this.supplier = null;
    }

    public static <T> Lazy<T> of(Supplier<? extends T> supplier) {
        return new Lazy<>(supplier);
    }

    /** To be used in the event that a value already exists. */
    public static <T> Lazy<T> of(T value) {
        return new Lazy<>(value);
    }

    @Override
    public T get() {
        if (this.supplier != null) {
            this.value = this.supplier.get();
            this.supplier = null;
        }
        return this.value;
    }

    /** Returns whether the underlying operation has completed. */
    public boolean computed() {
        return this.supplier == null;
    }

}
