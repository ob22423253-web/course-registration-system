package com.uni.registration.exception;

public class CourseNotFoundException extends RegistrationStateException {
    public CourseNotFoundException(String courseId) {
        super("COURSE_NOT_FOUND", "No course with id " + courseId);
    }
}
