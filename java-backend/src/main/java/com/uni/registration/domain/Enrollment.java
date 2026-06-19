package com.uni.registration.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

// Unique compound index on (studentId, courseId, active=true) is enforced in the service
// rather than DB-side because Mongo doesn't have partial-unique compound indices on the
// flapdoodle test image; the in-process duplicate check covers it.
@Document(collection = "enrollments")
@CompoundIndexes({
        @CompoundIndex(name = "student_course_idx", def = "{'studentId': 1, 'courseId': 1}")
})
public class Enrollment {

    @Id
    private String enrollmentId;

    @Indexed
    private String studentId;

    @Indexed
    private String courseId;

    private Instant enrolledAt;

    private EnrollmentState state;

    public Enrollment() { }

    public Enrollment(String enrollmentId, String studentId, String courseId,
                      Instant enrolledAt, EnrollmentState state) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.enrolledAt = enrolledAt;
        this.state = state;
    }

    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public Instant getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(Instant enrolledAt) { this.enrolledAt = enrolledAt; }

    public EnrollmentState getState() { return state; }
    public void setState(EnrollmentState state) { this.state = state; }

    public enum EnrollmentState {
        ENROLLED,
        WAITLISTED,
        DROPPED
    }
}
