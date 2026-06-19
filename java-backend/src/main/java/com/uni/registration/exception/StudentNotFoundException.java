package com.uni.registration.exception;

/**
 * Unchecked — if a controller asks for a student id that doesn't exist, that's a
 * client error we surface as 404 in the global handler. Not part of the checked
 * RegistrationException family because the registration path itself doesn't
 * recover from it.
 */
public class StudentNotFoundException extends RegistrationStateException {
    public StudentNotFoundException(String studentId) {
        super("STUDENT_NOT_FOUND", "No student with id " + studentId);
    }
}
