package com.uni.registration.exception;

public class DuplicateEnrollmentException extends RegistrationException {

    public DuplicateEnrollmentException(String studentId, String courseId) {
        super("DUPLICATE_ENROLLMENT",
                "Student " + studentId + " is already enrolled in " + courseId);
    }
}
