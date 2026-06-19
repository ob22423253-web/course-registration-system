package com.uni.registration.service;

import com.uni.registration.adt.HistoryStack;
import com.uni.registration.adt.Result;
import com.uni.registration.domain.Course;
import com.uni.registration.domain.Enrollment;
import com.uni.registration.domain.Student;
import com.uni.registration.dto.EligibilityRequest;
import com.uni.registration.dto.EligibilityResponse;
import com.uni.registration.dto.RegistrationResponse;
import com.uni.registration.exception.*;
import com.uni.registration.functional.RegistrationValidator;
import com.uni.registration.repository.CourseRepositoryAdapter;
import com.uni.registration.repository.EnrollmentRepositoryAdapter;
import com.uni.registration.repository.StudentRepositoryAdapter;
import com.uni.registration.util.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Heart of the system: orchestrates validation, capacity, persistence, waitlist,
 * and history. Public {@link #register} returns either an enrolled response or
 * (on CourseFullException) recovers by waitlisting — the prompt requires that
 * exception → recovery path explicitly.
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final StudentRepositoryAdapter students;
    private final CourseRepositoryAdapter courses;
    private final EnrollmentRepositoryAdapter enrollments;
    private final WaitlistService waitlist;
    private final PythonAnalyticsClient analytics;

    // One lock per course id. Required because seat-count updates must be atomic:
    // read-current → check capacity → write-incremented. Without this, two
    // concurrent registrations can both pass the "has seats" check and overbook.
    private final Map<String, ReentrantLock> courseLocks = new ConcurrentHashMap<>();

    // Per-student LIFO of registration history — used by undoLast().
    private final Map<String, HistoryStack<String>> history = new ConcurrentHashMap<>();

    public RegistrationService(StudentRepositoryAdapter students,
                               CourseRepositoryAdapter courses,
                               EnrollmentRepositoryAdapter enrollments,
                               WaitlistService waitlist,
                               PythonAnalyticsClient analytics) {
        this.students = students;
        this.courses = courses;
        this.enrollments = enrollments;
        this.waitlist = waitlist;
        this.analytics = analytics;
    }

    /**
     * Single-shot registration. Throws the *checked* business exceptions so the
     * caller (controller or executor) decides how to render them; the only
     * exception we recover from internally is CourseFullException → waitlist.
     */
    public RegistrationResponse register(String studentId, String courseId) throws RegistrationException {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        validate(student, course);

        try {
            String enrollmentId = enroll(student, course);
            log.info("ENROLLED student={} course={} enrollment={}", studentId, courseId, enrollmentId);
            return RegistrationResponse.enrolled(enrollmentId);
        } catch (CourseFullException ex) {
            // Recovery path: park them on the waitlist instead of bubbling up.
            int pos = waitlist.joinWaitlist(courseId, studentId);
            String wId = Ids.newId();
            enrollments.save(new Enrollment(wId, studentId, courseId, Instant.now(),
                    Enrollment.EnrollmentState.WAITLISTED));
            log.info("WAITLISTED student={} course={} position={}", studentId, courseId, pos);
            return RegistrationResponse.waitlisted(wId, pos);
        }
    }

    /**
     * Validation pipeline expressed as composed RegistrationValidator lambdas.
     * Each step returns Result so we can chain without exceptions for expected
     * outcomes. We *do* throw at the end so callers see the typed exception family.
     */
    private void validate(Student student, Course course) throws RegistrationException {
        RegistrationValidator notDuplicate = (s, c) ->
                enrollments.findByStudentIdAndCourseId(s.getStudentId(), c.getCourseId())
                        .filter(e -> e.getState() != Enrollment.EnrollmentState.DROPPED)
                        .<Result<Void>>map(e -> Result.<Void>failure("DUPLICATE", "already enrolled"))
                        .orElse(Result.success(null));

        RegistrationValidator activeStatus = (s, c) -> switch (s.getStatus()) {
            case SUSPENDED, GRADUATED ->
                    Result.failure("INELIGIBLE_STATUS", "student status " + s.getStatus() + " cannot register");
            default -> Result.success(null);
        };

        // Prereq check delegates to Python so we exercise the round-trip.
        RegistrationValidator prereq = (s, c) -> {
            if (c.getPrerequisiteRule() == null || c.getPrerequisiteRule().isBlank()) {
                return Result.success(null);
            }
            EligibilityRequest req = new EligibilityRequest(
                    s.getStudentId(), s.getStatus().name(), s.getGpa(),
                    s.getCompletedCourses(), c.getCourseId(), c.getPrerequisiteRule());
            EligibilityResponse resp = analytics.checkEligibility(req);
            return resp.isEligible() ? Result.success(null)
                    : Result.failure("PREREQ_NOT_MET", resp.getReason());
        };

        Result<Void> outcome = notDuplicate.andThen(activeStatus).andThen(prereq)
                .validate(student, course);

        if (!outcome.isSuccess()) {
            switch (outcome.errorCode()) {
                case "DUPLICATE":
                    throw new DuplicateEnrollmentException(student.getStudentId(), course.getCourseId());
                case "PREREQ_NOT_MET":
                    throw new PrerequisiteNotMetException(course.getCourseId(), course.getPrerequisiteCourses());
                default:
                    throw new RegistrationException(outcome.errorCode(), outcome.errorMessage());
            }
        }
    }

    /**
     * Capacity-checked enrollment. The per-course lock + version field on Course
     * are what guarantee no overbooking under concurrency.
     */
    private String enroll(Student student, Course course) throws CourseFullException {
        ReentrantLock lock = courseLocks.computeIfAbsent(course.getCourseId(), k -> new ReentrantLock(true));
        lock.lock();
        try {
            // Re-read inside the lock — the cached copy could be stale relative to other workers.
            Course fresh = courses.findById(course.getCourseId())
                    .orElseThrow(() -> new CourseNotFoundException(course.getCourseId()));

            if (!fresh.hasSeatsAvailable()) {
                throw new CourseFullException(fresh.getCourseId());
            }

            fresh.setEnrolledCount(fresh.getEnrolledCount() + 1);
            courses.save(fresh);

            String enrollmentId = Ids.newId();
            enrollments.save(new Enrollment(enrollmentId, student.getStudentId(),
                    course.getCourseId(), Instant.now(),
                    Enrollment.EnrollmentState.ENROLLED));
            history.computeIfAbsent(student.getStudentId(), k -> new HistoryStack<>())
                    .push(enrollmentId);
            return enrollmentId;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Drop a course: free a seat, persist the change, then notify the waitlist
     * consumer so the next person can be promoted. Returns false if not enrolled.
     */
    public boolean drop(String studentId, String courseId) {
        ReentrantLock lock = courseLocks.computeIfAbsent(courseId, k -> new ReentrantLock(true));
        lock.lock();
        try {
            Enrollment e = enrollments.findByStudentIdAndCourseId(studentId, courseId)
                    .filter(en -> en.getState() == Enrollment.EnrollmentState.ENROLLED)
                    .orElse(null);
            if (e == null) return false;
            e.setState(Enrollment.EnrollmentState.DROPPED);
            enrollments.save(e);

            Course c = courses.findById(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));
            c.setEnrolledCount(Math.max(0, c.getEnrolledCount() - 1));
            courses.save(c);

            // Hand off to producer-consumer: the consumer thread will promote the next waitlisted student.
            waitlist.publishSeatOpened(courseId);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Promote one waitlisted student into a freed seat. Called by the consumer thread.
     * Public because the consumer is in a different class.
     */
    public boolean promoteNext(String courseId) {
        ReentrantLock lock = courseLocks.computeIfAbsent(courseId, k -> new ReentrantLock(true));
        lock.lock();
        try {
            Course c = courses.findById(courseId).orElse(null);
            if (c == null || !c.hasSeatsAvailable()) return false;

            String nextStudent = waitlist.pollNextForCourse(courseId);
            if (nextStudent == null) return false;

            c.setEnrolledCount(c.getEnrolledCount() + 1);
            courses.save(c);

            // Reuse the existing WAITLISTED row so we never accumulate two enrollments
            // for the same (student, course). New row only if the student was never
            // recorded (shouldn't happen on the waitlist path, but defensive).
            Enrollment existing = enrollments.findByStudentIdAndCourseId(nextStudent, courseId).orElse(null);
            if (existing != null) {
                existing.setState(Enrollment.EnrollmentState.ENROLLED);
                existing.setEnrolledAt(Instant.now());
                enrollments.save(existing);
            } else {
                enrollments.save(new Enrollment(Ids.newId(), nextStudent, courseId, Instant.now(),
                        Enrollment.EnrollmentState.ENROLLED));
            }
            log.info("PROMOTED student={} into course={}", nextStudent, courseId);
            waitlist.notifyPromoted(courseId, nextStudent);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** Undo the most recent registration for the student using the history stack. */
    public boolean undoLast(String studentId) {
        HistoryStack<String> stack = history.get(studentId);
        if (stack == null || stack.isEmpty()) return false;
        String lastEnrollmentId = stack.pop();
        return enrollments.findById(lastEnrollmentId).map(e -> {
            if (e.getState() == Enrollment.EnrollmentState.ENROLLED) {
                return drop(e.getStudentId(), e.getCourseId());
            }
            return false;
        }).orElse(false);
    }

    public HistoryStack<String> historyOf(String studentId) {
        return history.computeIfAbsent(studentId, k -> new HistoryStack<>());
    }
}
