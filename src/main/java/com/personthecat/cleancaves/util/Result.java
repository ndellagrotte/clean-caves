package com.personthecat.cleancaves.util;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * An "error or nothing" box, used so that IO operations can report failure without throwing.
 * Deliberately not an {@code Either}: there is no success value to carry, only the absence or
 * presence of a {@link Throwable}.
 *
 * <p>The underlying operation has already run by the time a {@code Result} exists. This type is a
 * prompt to handle the error rather than a deferral of it -- {@code @CheckReturnValue} on the
 * producers in {@link com.personthecat.cleancaves.io.SafeFileIO} is what makes the prompt stick.
 *
 * @param error the error that occurred, or {@code null} when the operation succeeded
 */
public record Result<E extends Throwable>(@Nullable E error) {

    private static final Result<?> OK = new Result<>(null);

    /** Returns an instance with no error. See {@link Optional#empty}. */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> Result<E> ok() {
        return (Result<E>) OK;
    }

    /** Constructs a Result when an error is known to exist. */
    public static <E extends Throwable> Result<E> of(E error) {
        if (error == null) {
            throw CommonMethods.runEx("Error: Attempted to construct an error with no value present.");
        }
        return new Result<>(error);
    }

    /** Constructs a Result when an error is not known to exist. */
    public static <E extends Throwable> Result<E> ofUnknown(@Nullable E error) {
        return error == null ? ok() : new Result<>(error);
    }

    public boolean isErr() {
        return this.error != null;
    }

    public boolean isOk() {
        return this.error == null;
    }

    /** Calls the given function if an error is present. */
    public Result<E> handleIfPresent(Consumer<E> func) {
        if (this.error != null) {
            func.accept(this.error);
        }
        return this;
    }

    /** Runs the given function if no error is present. */
    public Result<E> andThen(Runnable func) {
        if (this.error == null) {
            func.run();
        }
        return this;
    }

    /** Throws the underlying exception, if present. */
    public void throwIfPresent() {
        if (this.error != null) {
            throw new RuntimeException(this.error);
        }
    }

    /** Throws the underlying exception with an error message. */
    public void expect(String message) {
        if (this.error != null) {
            throw new RuntimeException(message, this.error);
        }
    }

    /**
     * Throws the underlying exception with an interpolated error message. Uses {@code {}}
     * placeholders, matching every call site -- the original used {@link String#format}, which
     * silently discarded the arguments.
     */
    public void expectF(String message, Object... args) {
        if (this.error != null) {
            throw new RuntimeException(CommonMethods.f(message, args), this.error);
        }
    }

    /** Returns the error's message, if there is an error and it carries one. */
    public Optional<String> getMessage() {
        return this.error == null ? Optional.empty() : Optional.ofNullable(this.error.getMessage());
    }
}
