package com.uni.registration.adt;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Hand-rolled generic FIFO queue used as the per-course waitlist data structure.
 *
 * Why not java.util.LinkedList? Two reasons:
 *  1) Course requirement — implement ADTs ourselves, not just reuse built-ins.
 *  2) We need fair, ordered semantics and a single point to lock so position()
 *     and enqueue/dequeue stay consistent under concurrent calls.
 *
 * Singly-linked list with head/tail pointers — O(1) enqueue, O(1) dequeue,
 * O(n) position lookup (acceptable: waitlists rarely exceed a few hundred).
 */
public class WaitlistQueue<E> implements Iterable<E> {

    private static final class Node<E> {
        final E value;
        Node<E> next;
        Node(E value) { this.value = value; }
    }

    private Node<E> head;
    private Node<E> tail;
    private int size;

    // Single lock around all mutating ops — waitlist contention is low (one queue per course)
    // so a coarse lock is simpler and correct.
    private final ReentrantLock lock = new ReentrantLock(true);

    public void enqueue(E value) {
        lock.lock();
        try {
            Node<E> node = new Node<>(value);
            if (tail == null) {
                head = tail = node;
            } else {
                tail.next = node;
                tail = node;
            }
            size++;
        } finally {
            lock.unlock();
        }
    }

    /** Remove and return head; throws if empty so callers must check first. */
    public E dequeue() {
        lock.lock();
        try {
            if (head == null) throw new NoSuchElementException("waitlist empty");
            E value = head.value;
            head = head.next;
            if (head == null) tail = null;
            size--;
            return value;
        } finally {
            lock.unlock();
        }
    }

    public E peek() {
        lock.lock();
        try {
            return head == null ? null : head.value;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try { return size; } finally { lock.unlock(); }
    }

    public boolean isEmpty() { return size() == 0; }

    /**
     * 1-based position of the first occurrence of {@code value}, or -1 if absent.
     * Used to report waitlist position back to the student.
     */
    public int position(E value) {
        lock.lock();
        try {
            int idx = 1;
            for (Node<E> n = head; n != null; n = n.next, idx++) {
                if (n.value.equals(value)) return idx;
            }
            return -1;
        } finally {
            lock.unlock();
        }
    }

    /** Remove first equal occurrence (used when a waitlisted student withdraws). */
    public boolean remove(E value) {
        lock.lock();
        try {
            Node<E> prev = null;
            for (Node<E> n = head; n != null; prev = n, n = n.next) {
                if (n.value.equals(value)) {
                    if (prev == null) head = n.next; else prev.next = n.next;
                    if (n == tail) tail = prev;
                    size--;
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        // Snapshot iterator — avoids holding the lock during traversal by external code.
        lock.lock();
        try {
            java.util.ArrayList<E> snap = new java.util.ArrayList<>(size);
            for (Node<E> n = head; n != null; n = n.next) snap.add(n.value);
            return snap.iterator();
        } finally {
            lock.unlock();
        }
    }
}
