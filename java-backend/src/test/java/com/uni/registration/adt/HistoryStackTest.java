package com.uni.registration.adt;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class HistoryStackTest {

    @Test
    void lifo_order() {
        HistoryStack<String> s = new HistoryStack<>();
        s.push("a"); s.push("b"); s.push("c");
        assertEquals("c", s.pop());
        assertEquals("b", s.pop());
        assertEquals("a", s.pop());
        assertTrue(s.isEmpty());
    }

    @Test
    void peek_does_not_remove() {
        HistoryStack<Integer> s = new HistoryStack<>();
        s.push(1); s.push(2);
        assertEquals(2, s.peek());
        assertEquals(2, s.size());
    }

    @Test
    void pop_empty_throws() {
        assertThrows(NoSuchElementException.class, () -> new HistoryStack<>().pop());
    }
}
