package com.uni.registration.adt;

import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Result&lt;T&gt; — a tiny sum type (success | failure). We use it instead of throwing
 * for *expected* business outcomes (course full, prereq not met, etc.) so service
 * code can compose them with map/flatMap. Unexpected faults still throw.
 *
 * Kept as a sealed hierarchy so the compiler enforces exhaustive handling in switch.
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    static <T> Result<T> success(T value) { return new Success<>(value); }
    static <T> Result<T> failure(String code, String message) { return new Failure<>(code, message); }

    boolean isSuccess();

    T get();
    String errorCode();
    String errorMessage();

    /** Map the success value; failures pass through unchanged. */
    <R> Result<R> map(Function<? super T, ? extends R> mapper);

    /** Chain another Result-producing operation; failures short-circuit. */
    <R> Result<R> flatMap(Function<? super T, Result<R>> mapper);

    record Success<T>(T value) implements Result<T> {
        public boolean isSuccess() { return true; }
        public T get() { return value; }
        public String errorCode() { return null; }
        public String errorMessage() { return null; }

        @SuppressWarnings("unchecked")
        public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
            return new Success<>(mapper.apply(value));
        }
        @SuppressWarnings("unchecked")
        public <R> Result<R> flatMap(Function<? super T, Result<R>> mapper) {
            return mapper.apply(value);
        }
    }

    record Failure<T>(String code, String message) implements Result<T> {
        public boolean isSuccess() { return false; }
        public T get() { throw new NoSuchElementException("Result is a failure: " + code + " " + message); }
        public String errorCode() { return code; }
        public String errorMessage() { return message; }

        @SuppressWarnings("unchecked")
        public <R> Result<R> map(Function<? super T, ? extends R> mapper) {
            return (Result<R>) this;
        }
        @SuppressWarnings("unchecked")
        public <R> Result<R> flatMap(Function<? super T, Result<R>> mapper) {
            return (Result<R>) this;
        }
    }
}
