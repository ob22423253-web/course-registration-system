package com.uni.registration.service;

import com.uni.registration.domain.Course;
import com.uni.registration.domain.Enrollment;
import com.uni.registration.domain.Student;
import com.uni.registration.domain.StudentStatus;
import com.uni.registration.dto.EligibilityResponse;
import com.uni.registration.dto.RegistrationResponse;
import com.uni.registration.exception.DuplicateEnrollmentException;
import com.uni.registration.exception.RegistrationException;
import com.uni.registration.observer.LoggingNotificationObserver;
import com.uni.registration.observer.WaitlistSubject;
import com.uni.registration.repository.CourseRepositoryAdapter;
import com.uni.registration.repository.EnrollmentRepositoryAdapter;
import com.uni.registration.repository.StudentRepositoryAdapter;
import com.uni.registration.support.InMemoryCourseRepo;
import com.uni.registration.support.InMemoryEnrollmentRepo;
import com.uni.registration.support.InMemoryStudentRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-Java wiring: no Spring context, no database. We construct services with
 * in-memory repos so the test is fast and deterministic.
 */
class RegistrationServiceTest {

    private RegistrationService service;
    private StudentRepositoryAdapter students;
    private CourseRepositoryAdapter courses;
    private EnrollmentRepositoryAdapter enrollments;
    private WaitlistService waitlist;
    private PythonAnalyticsClient analytics;

    @BeforeEach
    void setUp() {
        students = new StudentRepositoryAdapter(new InMemoryStudentRepo());
        courses = new CourseRepositoryAdapter(new InMemoryCourseRepo());
        enrollments = new EnrollmentRepositoryAdapter(new InMemoryEnrollmentRepo());
        WaitlistSubject subject = new WaitlistSubject(List.of(new LoggingNotificationObserver()));
        waitlist = new WaitlistService(subject);
        analytics = Mockito.mock(PythonAnalyticsClient.class);

        EligibilityResponse ok = new EligibilityResponse();
        ok.setEligible(true); ok.setReason("ok");
        Mockito.when(analytics.checkEligibility(Mockito.any())).thenReturn(ok);

        service = new RegistrationService(students, courses, enrollments, waitlist, analytics);
    }

    @Test
    void happy_path_enrolls() throws Exception {
        students.save(new Student("s1", "A", "B", "a@b.com", StudentStatus.ACTIVE, 3.5, List.of("CS101")));
        courses.save(new Course("CS201", "Algorithms", 30, 3, "CS101", List.of("CS101")));

        RegistrationResponse r = service.register("s1", "CS201");

        assertEquals(RegistrationResponse.Outcome.ENROLLED, r.getOutcome());
        assertEquals(1, courses.findById("CS201").orElseThrow().getEnrolledCount());
    }

    @Test
    void duplicate_enrollment_throws() throws Exception {
        students.save(new Student("s1", "A", "B", "a@b.com", StudentStatus.ACTIVE, 3.5, List.of("CS101")));
        courses.save(new Course("CS201", "Algorithms", 30, 3, "CS101", List.of("CS101")));

        service.register("s1", "CS201");
        assertThrows(DuplicateEnrollmentException.class, () -> service.register("s1", "CS201"));
    }

    @Test
    void full_course_routes_to_waitlist() throws Exception {
        students.save(new Student("s1", "A", "B", "a@b.com", StudentStatus.ACTIVE, 3.5, List.of()));
        students.save(new Student("s2", "B", "C", "c@b.com", StudentStatus.ACTIVE, 3.5, List.of()));
        courses.save(new Course("CS900", "Tiny", 1, 3, "", List.of()));

        RegistrationResponse first = service.register("s1", "CS900");
        RegistrationResponse second = service.register("s2", "CS900");

        assertEquals(RegistrationResponse.Outcome.ENROLLED, first.getOutcome());
        assertEquals(RegistrationResponse.Outcome.WAITLISTED, second.getOutcome());
        assertEquals(1, second.getWaitlistPosition());
    }

    @Test
    void drop_promotes_waitlisted_student() throws Exception {
        students.save(new Student("s1", "A", "B", "a@b.com", StudentStatus.ACTIVE, 3.5, List.of()));
        students.save(new Student("s2", "B", "C", "c@b.com", StudentStatus.ACTIVE, 3.5, List.of()));
        courses.save(new Course("CS900", "Tiny", 1, 3, "", List.of()));

        service.register("s1", "CS900");
        service.register("s2", "CS900");
        assertEquals(1, waitlist.size("CS900"));

        assertTrue(service.drop("s1", "CS900"));
        assertTrue(service.promoteNext("CS900"));

        Enrollment s2enrollment = enrollments.findByStudentIdAndCourseId("s2", "CS900").orElseThrow();
        assertEquals(Enrollment.EnrollmentState.ENROLLED, s2enrollment.getState());
    }

    @Test
    void prereq_failure_throws_typed_exception() {
        students.save(new Student("s1", "A", "B", "a@b.com", StudentStatus.ACTIVE, 3.5, List.of()));
        courses.save(new Course("CS201", "Algorithms", 30, 3, "CS101", List.of("CS101")));

        EligibilityResponse no = new EligibilityResponse();
        no.setEligible(false); no.setReason("prereqs missing");
        Mockito.when(analytics.checkEligibility(Mockito.any())).thenReturn(no);

        assertThrows(RegistrationException.class, () -> service.register("s1", "CS201"));
    }

    @Test
    void undo_drops_last_enrollment() throws Exception {
        students.save(new Student("s1", "A", "B", "a@b.com", StudentStatus.ACTIVE, 3.5, List.of("CS101")));
        courses.save(new Course("CS201", "Algorithms", 30, 3, "CS101", List.of("CS101")));

        service.register("s1", "CS201");
        assertTrue(service.undoLast("s1"));
        assertEquals(0, courses.findById("CS201").orElseThrow().getEnrolledCount());
    }
}
