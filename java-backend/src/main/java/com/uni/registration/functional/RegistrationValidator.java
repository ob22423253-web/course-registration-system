package com.uni.registration.functional;

import com.uni.registration.adt.Result;
import com.uni.registration.domain.Course;
import com.uni.registration.domain.Student;

/**
 * Single-method validator. Marked @FunctionalInterface so the compiler enforces
 * the lambda-friendly shape — we compose chains of these in RegistrationService
 * via {@link #andThen}, which gives us declarative pipelines rather than nested ifs.
 */
@FunctionalInterface
public interface RegistrationValidator {

    Result<Void> validate(Student student, Course course);

    /** Short-circuit composition — first failure wins, mirroring && semantics. */
    default RegistrationValidator andThen(RegistrationValidator next) {
        return (s, c) -> {
            Result<Void> first = validate(s, c);
            return first.isSuccess() ? next.validate(s, c) : first;
        };
    }
}
