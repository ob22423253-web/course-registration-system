package com.uni.registration.exception;

/**
 * Thrown when a course has no remaining seats. The registration service catches
 * this and recovers by enqueuing the student onto the waitlist — see
 * RegistrationService#register.
 */
public class CourseFullException extends RegistrationException {

    private final String courseId;

    public CourseFullException(String courseId) {
        super("COURSE_FULL", "Course " + courseId + " has no remaining seats");
        this.courseId = courseId;
    }

    public String getCourseId() { return courseId; }
}
