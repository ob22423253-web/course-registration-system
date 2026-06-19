package com.uni.registration.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Document(collection = "students")
public class Student extends Person {

    @Id
    private String studentId;

    // Indexed because we look students up by email during login flows.
    @Indexed(unique = true)
    private String emailIndex;

    private StudentStatus status;

    // GPA is computed by the Python service; the field here is a cached snapshot.
    private double gpa;

    // Completed courses drive prerequisite checks. Stored as IDs to keep the doc small.
    private List<String> completedCourses = new ArrayList<>();

    public Student() {
        super();
    }

    public Student(String studentId, String firstName, String lastName, String email,
                   StudentStatus status, double gpa, List<String> completedCourses) {
        super(studentId, firstName, lastName, email);
        this.studentId = studentId;
        this.emailIndex = email;
        this.status = status;
        this.gpa = gpa;
        this.completedCourses = completedCourses == null ? new ArrayList<>() : completedCourses;
    }

    @Override
    public String role() { return "STUDENT"; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) {
        this.studentId = studentId;
        setId(studentId);
    }

    public String getEmailIndex() { return emailIndex; }
    public void setEmailIndex(String emailIndex) { this.emailIndex = emailIndex; }

    public StudentStatus getStatus() { return status; }
    public void setStatus(StudentStatus status) { this.status = status; }

    public double getGpa() { return gpa; }
    public void setGpa(double gpa) { this.gpa = gpa; }

    // Exposed as unmodifiable to prevent outside code from mutating our state directly —
    // mutation must go through addCompletedCourse so we control invariants.
    public List<String> getCompletedCourses() {
        return Collections.unmodifiableList(completedCourses);
    }

    public void setCompletedCourses(List<String> completedCourses) {
        this.completedCourses = completedCourses == null ? new ArrayList<>() : new ArrayList<>(completedCourses);
    }

    public void addCompletedCourse(String courseId) {
        if (!completedCourses.contains(courseId)) completedCourses.add(courseId);
    }
}
