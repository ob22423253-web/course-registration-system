package com.uni.registration.service.concurrent;

import com.uni.registration.dto.RegistrationResponse;
import com.uni.registration.exception.RegistrationException;
import com.uni.registration.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Lets controllers fire many registration requests at the dedicated thread pool.
 * Returns CompletableFutures so the caller can await one, all, or any.
 * Each task runs RegistrationService.register, which internally locks per-course
 * to keep capacity correct.
 */
@Component
public class RegistrationExecutor {

    private static final Logger log = LoggerFactory.getLogger(RegistrationExecutor.class);

    private final RegistrationService service;
    private final Executor executor;

    public RegistrationExecutor(RegistrationService service,
                                @Qualifier("registrationTaskExecutor") Executor executor) {
        this.service = service;
        this.executor = executor;
    }

    /** Schedule one registration on the pool. */
    public CompletableFuture<RegistrationResponse> submit(String studentId, String courseId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return service.register(studentId, courseId);
            } catch (RegistrationException ex) {
                log.debug("registration rejected for {}/{}: {}", studentId, courseId, ex.getMessage());
                return RegistrationResponse.rejected(ex.getMessage());
            }
        }, executor);
    }

    /** Bulk submit — used by the stress test to fan out hundreds of requests. */
    public List<RegistrationResponse> submitAllAndWait(List<RegistrationRequestPair> reqs) {
        List<CompletableFuture<RegistrationResponse>> futures =
                reqs.stream().map(r -> submit(r.studentId(), r.courseId())).collect(Collectors.toList());
        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    public record RegistrationRequestPair(String studentId, String courseId) { }
}
