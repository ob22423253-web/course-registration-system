package com.uni.registration.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Functional stream helpers shared across services. Pure functions only —
 * keeps the call sites readable and testable.
 */
public final class Streams {
    private Streams() {}

    /** Filter+map in one pass — saves writing two .stream() calls at most sites. */
    public static <T, R> List<R> filterMap(Collection<T> in,
                                           Predicate<? super T> keep,
                                           Function<? super T, ? extends R> map) {
        return in.stream().filter(keep).map(map).collect(Collectors.toList());
    }

    public static <T> List<T> sorted(Collection<T> in, Comparator<? super T> by) {
        return in.stream().sorted(by).collect(Collectors.toList());
    }
}
