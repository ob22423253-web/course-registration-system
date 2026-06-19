package com.uni.registration.observer;

import java.time.Instant;

/**
 * What observers receive when a seat opens. Immutable record so listeners
 * cannot mutate cross-listener state.
 */
public record WaitlistEvent(String courseId, String promotedStudentId, Instant occurredAt) {
    public static WaitlistEvent now(String courseId, String studentId) {
        // We pass Instant.now() at construction to keep the record itself pure.
        return new WaitlistEvent(courseId, studentId, Instant.now());
    }
}
