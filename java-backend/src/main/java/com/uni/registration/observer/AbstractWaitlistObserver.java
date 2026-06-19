package com.uni.registration.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inheritance use-case — concrete observers (email, SMS, push) share the same
 * audit-log call so they can't accidentally forget to log a delivery. Subclasses
 * only implement deliver().
 */
public abstract class AbstractWaitlistObserver implements WaitlistObserver {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public final void onSeatPromoted(WaitlistEvent event) {
        log.info("notifying student={} for course={} via {}",
                event.promotedStudentId(), event.courseId(), channel());
        try {
            deliver(event);
        } catch (RuntimeException ex) {
            // Observer failures must not break the registration thread.
            log.error("observer {} failed for student {}", channel(), event.promotedStudentId(), ex);
        }
    }

    protected abstract String channel();
    protected abstract void deliver(WaitlistEvent event);
}
