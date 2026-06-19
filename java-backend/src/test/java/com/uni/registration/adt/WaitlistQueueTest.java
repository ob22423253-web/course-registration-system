package com.uni.registration.adt;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WaitlistQueueTest {

    @Test
    void fifo_order_preserved() {
        WaitlistQueue<String> q = new WaitlistQueue<>();
        q.enqueue("a"); q.enqueue("b"); q.enqueue("c");
        assertEquals("a", q.dequeue());
        assertEquals("b", q.dequeue());
        assertEquals("c", q.dequeue());
        assertTrue(q.isEmpty());
    }

    @Test
    void position_is_one_based() {
        WaitlistQueue<String> q = new WaitlistQueue<>();
        q.enqueue("s1"); q.enqueue("s2"); q.enqueue("s3");
        assertEquals(1, q.position("s1"));
        assertEquals(3, q.position("s3"));
        assertEquals(-1, q.position("nope"));
    }

    @Test
    void remove_compacts_correctly() {
        WaitlistQueue<String> q = new WaitlistQueue<>();
        q.enqueue("a"); q.enqueue("b"); q.enqueue("c");
        assertTrue(q.remove("b"));
        assertEquals(1, q.position("a"));
        assertEquals(2, q.position("c"));
        assertEquals(2, q.size());
    }

    @Test
    void dequeue_empty_throws() {
        assertThrows(NoSuchElementException.class, () -> new WaitlistQueue<>().dequeue());
    }

    @Test
    void concurrent_enqueue_preserves_size() throws InterruptedException {
        WaitlistQueue<Integer> q = new WaitlistQueue<>();
        int threads = 16;
        int per = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger();
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try { start.await(); } catch (InterruptedException ignored) {}
                for (int i = 0; i < per; i++) q.enqueue(counter.incrementAndGet());
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(threads * per, q.size());
    }
}
