package com.uni.registration.observer;

import org.springframework.stereotype.Component;

/**
 * The default "notification channel" — logs the event. In production we'd swap
 * in EmailNotificationObserver / PushNotificationObserver; the pattern keeps
 * RegistrationService unaware of which channels exist.
 */
@Component
public class LoggingNotificationObserver extends AbstractWaitlistObserver {

    @Override
    protected String channel() { return "LOG"; }

    @Override
    protected void deliver(WaitlistEvent event) {
        log.info("[NOTIFY] student {} promoted to course {} at {}",
                event.promotedStudentId(), event.courseId(), event.occurredAt());
    }
}
