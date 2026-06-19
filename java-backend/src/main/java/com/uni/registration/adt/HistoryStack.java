package com.uni.registration.adt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Generic LIFO stack used for per-student registration history so we can support
 * "undo last registration" without scanning the whole enrollments collection.
 *
 * Backed by an ArrayList for amortised O(1) push/pop. Not thread-safe per instance —
 * intended for single-student contexts (one stack per student in the in-memory cache).
 * If we ever share across threads, callers must synchronize externally.
 */
public class HistoryStack<E> implements Iterable<E> {

    private final List<E> data = new ArrayList<>();

    public void push(E value) {
        data.add(value);
    }

    public E pop() {
        if (data.isEmpty()) throw new NoSuchElementException("history empty");
        return data.remove(data.size() - 1);
    }

    public E peek() {
        if (data.isEmpty()) return null;
        return data.get(data.size() - 1);
    }

    public int size() { return data.size(); }

    public boolean isEmpty() { return data.isEmpty(); }

    public void clear() { data.clear(); }

    /** Iteration order is bottom→top, matching audit-log presentation. */
    @Override
    public Iterator<E> iterator() {
        return data.iterator();
    }
}
