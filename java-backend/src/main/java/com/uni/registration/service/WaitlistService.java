package com.uni.registration.service;

import com.uni.registration.adt.WaitlistQueue;
import com.uni.registration.observer.WaitlistEvent;
import com.uni.registration.observer.WaitlistSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Owns per-course waitlist FIFO queues *and* the global promotion-event queue
 * the consumer thread reads from. Two structures, two purposes:
 *
 *   - WaitlistQueue (custom ADT): the visible "I'm in position 4" queue for a course.
 *   - BlockingQueue (JDK): the producer-consumer channel — when a seat opens we
 *     publish a PromotionTrigger here and the consumer thread acts on it.
 *
 * We split them so the consumer doesn't need to scan every course every poll —
 * it blocks on the trigger queue and wakes only when there is work.
 */
@Service
public class WaitlistService {

    private static final Logger log = LoggerFactory.getLogger(WaitlistService.class);

    private final Map<String, WaitlistQueue<String>> perCourse = new ConcurrentHashMap<>();
    private final BlockingQueue<PromotionTrigger> triggers = new LinkedBlockingQueue<>();
    private final WaitlistSubject subject;

    public WaitlistService(WaitlistSubject subject) {
        this.subject = subject;
    }

    /** Producer side: a student couldn't get a seat, so we park them on the waitlist. */
    public int joinWaitlist(String courseId, String studentId) {
        WaitlistQueue<String> q = perCourse.computeIfAbsent(courseId, k -> new WaitlistQueue<>());
        q.enqueue(studentId);
        int pos = q.position(studentId);
        log.info("student {} joined waitlist for {} at position {}", studentId, courseId, pos);
        return pos;
    }

    /** Producer side (for the consumer thread): a seat opened — promotion needed. */
    public void publishSeatOpened(String courseId) {
        triggers.offer(new PromotionTrigger(courseId));
    }

    /** Consumer side: blocks until a trigger arrives, returns the next courseId to handle. */
    public PromotionTrigger takeTrigger() throws InterruptedException {
        return triggers.take();
    }

    /** Returns the next waitlisted studentId for {@code courseId}, or null if empty. */
    public String pollNextForCourse(String courseId) {
        WaitlistQueue<String> q = perCourse.get(courseId);
        if (q == null || q.isEmpty()) return null;
        try {
            return q.dequeue();
        } catch (java.util.NoSuchElementException e) {
            return null;
        }
    }

    public int position(String courseId, String studentId) {
        WaitlistQueue<String> q = perCourse.get(courseId);
        return q == null ? -1 : q.position(studentId);
    }

    public boolean cancel(String courseId, String studentId) {
        WaitlistQueue<String> q = perCourse.get(courseId);
        return q != null && q.remove(studentId);
    }

    public int size(String courseId) {
        WaitlistQueue<String> q = perCourse.get(courseId);
        return q == null ? 0 : q.size();
    }

    public void notifyPromoted(String courseId, String studentId) {
        subject.fire(WaitlistEvent.now(courseId, studentId));
    }

    /** Visible for tests so we can assert producer-consumer flow. */
    public BlockingQueue<PromotionTrigger> triggers() { return triggers; }

    public record PromotionTrigger(String courseId) { }
}
