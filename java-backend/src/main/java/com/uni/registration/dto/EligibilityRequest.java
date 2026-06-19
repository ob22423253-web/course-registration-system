package com.uni.registration.dto;

import java.util.List;

/**
 * Payload Java sends to Python so the analytics service can decide
 * if the student satisfies a course's prerequisite rule.
 */
public class EligibilityRequest {

    private String studentId;
    private String status;
    private double gpa;
    private List<String> completedCourses;
    private String courseId;
    private String prerequisiteRule;

    public EligibilityRequest() { }

    public EligibilityRequest(String studentId, String status, double gpa,
                              List<String> completedCourses, String courseId, String prerequisiteRule) {
        this.studentId = studentId;
        this.status = status;
        this.gpa = gpa;
        this.completedCourses = completedCourses;
        this.courseId = courseId;
        this.prerequisiteRule = prerequisiteRule;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getGpa() { return gpa; }
    public void setGpa(double gpa) { this.gpa = gpa; }
    public List<String> getCompletedCourses() { return completedCourses; }
    public void setCompletedCourses(List<String> completedCourses) { this.completedCourses = completedCourses; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getPrerequisiteRule() { return prerequisiteRule; }
    public void setPrerequisiteRule(String prerequisiteRule) { this.prerequisiteRule = prerequisiteRule; }
}
