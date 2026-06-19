package com.uni.registration.exception;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionHierarchyTest {

    @Test
    void checked_family_is_assignable_from_subtypes() {
        assertTrue(RegistrationException.class.isAssignableFrom(CourseFullException.class));
        assertTrue(RegistrationException.class.isAssignableFrom(PrerequisiteNotMetException.class));
        assertTrue(RegistrationException.class.isAssignableFrom(DuplicateEnrollmentException.class));
        // Base is checked (Exception, not RuntimeException).
        assertFalse(RuntimeException.class.isAssignableFrom(RegistrationException.class));
    }

    @Test
    void unchecked_family_is_runtime() {
        assertTrue(RuntimeException.class.isAssignableFrom(StudentNotFoundException.class));
        assertTrue(RuntimeException.class.isAssignableFrom(CourseNotFoundException.class));
    }

    @Test
    void messages_carry_codes() {
        CourseFullException ex = new CourseFullException("CS101");
        assertEquals("COURSE_FULL", ex.getCode());

        PrerequisiteNotMetException p = new PrerequisiteNotMetException("CS101", List.of("MATH110"));
        assertEquals("PREREQ_NOT_MET", p.getCode());
    }
}
