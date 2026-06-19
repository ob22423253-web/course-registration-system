package com.uni.registration.dto;

import jakarta.validation.constraints.NotBlank;

public class RegistrationRequest {

    @NotBlank
    private String studentId;

    @NotBlank
    private String courseId;

    public RegistrationRequest() { }

    public RegistrationRequest(String studentId, String courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
}
