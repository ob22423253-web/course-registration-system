package com.uni.registration.exception;

import java.util.List;

public class PrerequisiteNotMetException extends RegistrationException {

    private final String courseId;
    private final List<String> missing;

    public PrerequisiteNotMetException(String courseId, List<String> missing) {
        super("PREREQ_NOT_MET", "Missing prerequisites for " + courseId + ": " + missing);
        this.courseId = courseId;
        this.missing = missing;
    }

    public String getCourseId() { return courseId; }
    public List<String> getMissing() { return missing; }
}
