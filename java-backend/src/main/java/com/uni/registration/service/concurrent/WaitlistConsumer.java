package com.uni.registration.service.concurrent;

import com.uni.registration.service.RegistrationService;
import com.uni.registration.service.WaitlistService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Producer-consumer consumer side. Runs on its own daemon thread, blocks on the
 * WaitlistService's BlockingQueue until a seat-opened trigger arrives, then asks
 * RegistrationService to promote one waitlisted student.
 *
 * Why a dedicated thread instead of doing the promotion inline in drop()?
 *   - Keeps drop() fast and predictable; observers (notifications) run off the
 *     hot path.
 *   - Centralises promotion so we can later batch or rate-limit it.
 */
@Component
public class WaitlistConsumer {

    private static final Logger log = LoggerFactory.getLogger(WaitlistConsumer.class);

    private final WaitlistService waitlist;
    private final RegistrationService registration;

    private Thread worker;
    private volatile boolean running;

    public WaitlistConsumer(WaitlistService waitlist, RegistrationService registration) {
        this.waitlist = waitlist;
        this.registration = registration;
    }

    @PostConstruct
    public void start() {
        running = true;
        worker = new Thread(this::loop, "waitlist-consumer");
        // Daemon so JVM shutdown is not blocked if we forget to stop() explicitly.
        worker.setDaemon(true);
        worker.start();
        log.info("waitlist consumer started");
    }

    private void loop() {
        while (running) {
            try {
                WaitlistService.PromotionTrigger trigger = waitlist.takeTrigger();
                // Drain as many promotions as possible for this course — one drop()
                // can free a seat that unblocks multiple consecutive waitlisters
                // only if more drops follow, but we still try once defensively.
                boolean promoted = registration.promoteNext(trigger.courseId());
                if (!promoted) {
                    log.debug("no promotion happened for course {} (empty waitlist or no seats)",
                            trigger.courseId());
                }
            } catch (InterruptedException e) {
                // Restore interrupt status and exit cleanly on shutdown.
                Thread.currentThread().interrupt();
                break;
            } catch (RuntimeException ex) {
                // Don't kill the consumer on a single bad promotion — log and continue.
                log.error("waitlist consumer error", ex);
            }
        }
        log.info("waitlist consumer stopped");
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) worker.interrupt();
    }
}
