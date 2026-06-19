package com.uni.registration.service;

import com.uni.registration.domain.Course;
import com.uni.registration.domain.Student;
import com.uni.registration.domain.StudentStatus;
import com.uni.registration.dto.EligibilityResponse;
import com.uni.registration.dto.RegistrationResponse;
import com.uni.registration.exception.RegistrationException;
import com.uni.registration.observer.LoggingNotificationObserver;
import com.uni.registration.observer.WaitlistSubject;
import com.uni.registration.repository.CourseRepositoryAdapter;
import com.uni.registration.repository.EnrollmentRepositoryAdapter;
import com.uni.registration.repository.StudentRepositoryAdapter;
import com.uni.registration.service.concurrent.RegistrationExecutor;
import com.uni.registration.support.InMemoryCourseRepo;
import com.uni.registration.support.InMemoryEnrollmentRepo;
import com.uni.registration.support.InMemoryStudentRepo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hammers the registration pipeline with N concurrent students racing for a
 * course with K seats. Asserts:
 *   - exactly K enrolled (no overbooking)
 *   - exactly N-K waitlisted (no lost registrations)
 *
 * Prints throughput + latency. Class name ends in *StressTest so default
 * Surefire excludes it; run with `mvn -Dtest=*StressTest test`.
 */
class ConcurrentRegistrationStressTest {

    private RegistrationService buildService(StudentRepositoryAdapter sr,
                                             CourseRepositoryAdapter cr,
                                             EnrollmentRepositoryAdapter er,
                                             WaitlistService waitlist) {
        PythonAnalyticsClient analytics = Mockito.mock(PythonAnalyticsClient.class);
        EligibilityResponse ok = new EligibilityResponse();
        ok.setEligible(true);
        Mockito.when(analytics.checkEligibility(Mockito.any())).thenReturn(ok);
        return new RegistrationService(sr, cr, er, waitlist, analytics);
    }

    private Executor buildExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(8);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(500);
        ex.setThreadNamePrefix("stress-");
        ex.initialize();
        return ex;
    }

    @Test
    void no_overbooking_under_concurrent_load() {
        StudentRepositoryAdapter students = new StudentRepositoryAdapter(new InMemoryStudentRepo());
        CourseRepositoryAdapter courses = new CourseRepositoryAdapter(new InMemoryCourseRepo());
        EnrollmentRepositoryAdapter enrollments = new EnrollmentRepositoryAdapter(new InMemoryEnrollmentRepo());
        WaitlistSubject subject = new WaitlistSubject(List.of(new LoggingNotificationObserver()));
        WaitlistService waitlist = new WaitlistService(subject);
        RegistrationService service = buildService(students, courses, enrollments, waitlist);
        RegistrationExecutor executor = new RegistrationExecutor(service, buildExecutor());

        int N = 200;
        int K = 30;
        String courseId = "CS500";

        for (int i = 0; i < N; i++) {
            students.save(new Student("stress-" + i, "X", "Y", "x" + i + "@u.edu",
                    StudentStatus.ACTIVE, 3.0, List.of()));
        }
        courses.save(new Course(courseId, "Stress", K, 3, "", List.of()));

        long t0 = System.nanoTime();
        List<CompletableFuture<RegistrationResponse>> futures = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            futures.add(executor.submit("stress-" + i, courseId));
        }
        List<RegistrationResponse> results = futures.stream()
                .map(CompletableFuture::join).collect(Collectors.toList());
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;

        long enrolled = results.stream()
                .filter(r -> r.getOutcome() == RegistrationResponse.Outcome.ENROLLED).count();
        long waitlisted = results.stream()
                .filter(r -> r.getOutcome() == RegistrationResponse.Outcome.WAITLISTED).count();
        long rejected = results.stream()
                .filter(r -> r.getOutcome() == RegistrationResponse.Outcome.REJECTED).count();

        System.out.println("==== STRESS RESULTS ====");
        System.out.println("racers           : " + N);
        System.out.println("seats            : " + K);
        System.out.println("elapsed (ms)     : " + elapsedMs);
        System.out.println("throughput (r/s) : " + String.format("%.1f", N * 1000.0 / Math.max(1, elapsedMs)));
        System.out.println("avg latency (ms) : " + String.format("%.2f", (double) elapsedMs / N));
        System.out.println("ENROLLED         : " + enrolled);
        System.out.println("WAITLISTED       : " + waitlisted);
        System.out.println("REJECTED         : " + rejected);

        assertEquals(K, enrolled, "must be exactly K enrolled — no overbooking");
        assertEquals(N - K, waitlisted, "every non-enrolled must be waitlisted, none lost");
        assertEquals(0, rejected);
        assertEquals(K, courses.findById(courseId).orElseThrow().getEnrolledCount());
    }

    @Test
    void single_thread_baseline_for_comparison() throws RegistrationException {
        StudentRepositoryAdapter students = new StudentRepositoryAdapter(new InMemoryStudentRepo());
        CourseRepositoryAdapter courses = new CourseRepositoryAdapter(new InMemoryCourseRepo());
        EnrollmentRepositoryAdapter enrollments = new EnrollmentRepositoryAdapter(new InMemoryEnrollmentRepo());
        WaitlistSubject subject = new WaitlistSubject(List.of(new LoggingNotificationObserver()));
        WaitlistService waitlist = new WaitlistService(subject);
        RegistrationService service = buildService(students, courses, enrollments, waitlist);

        int N = 200;
        int K = 30;
        String courseId = "CS501";
        for (int i = 0; i < N; i++) {
            students.save(new Student("seq-" + i, "X", "Y", "y" + i + "@u.edu",
                    StudentStatus.ACTIVE, 3.0, List.of()));
        }
        courses.save(new Course(courseId, "Seq", K, 3, "", List.of()));

        long t0 = System.nanoTime();
        for (int i = 0; i < N; i++) service.register("seq-" + i, courseId);
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;

        System.out.println("==== SEQUENTIAL BASELINE ====");
        System.out.println("racers           : " + N);
        System.out.println("seats            : " + K);
        System.out.println("elapsed (ms)     : " + elapsedMs);
        System.out.println("throughput (r/s) : " + String.format("%.1f", N * 1000.0 / Math.max(1, elapsedMs)));

        assertEquals(K, courses.findById(courseId).orElseThrow().getEnrolledCount());
    }
}
