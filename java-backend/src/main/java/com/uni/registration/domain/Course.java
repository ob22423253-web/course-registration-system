package com.uni.registration.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Document(collection = "courses")
public class Course {

    @Id
    private String courseId;

    private String title;
    private int capacity;

    // Seats filled. We use Mongo's @Version for optimistic locking on capacity changes —
    // the in-process lock prevents races within a JVM, @Version prevents stale writes
    // across restarts / hypothetical multi-instance deployments.
    private int enrolledCount;

    @Version
    private Long version;

    // Stored as a raw rule string in the EBNF grammar; the Python parser owns evaluation.
    private String prerequisiteRule;

    // Cached completed list of prereq course IDs (extracted by the parser) for fast lookups.
    private List<String> prerequisiteCourses = new ArrayList<>();

    private int credits;

    public Course() { }

    public Course(String courseId, String title, int capacity, int credits,
                  String prerequisiteRule, List<String> prerequisiteCourses) {
        this.courseId = courseId;
        this.title = title;
        this.capacity = capacity;
        this.credits = credits;
        this.prerequisiteRule = prerequisiteRule;
        this.prerequisiteCourses = prerequisiteCourses == null ? new ArrayList<>() : prerequisiteCourses;
        this.enrolledCount = 0;
    }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getPrerequisiteRule() { return prerequisiteRule; }
    public void setPrerequisiteRule(String prerequisiteRule) { this.prerequisiteRule = prerequisiteRule; }

    public List<String> getPrerequisiteCourses() {
        return Collections.unmodifiableList(prerequisiteCourses);
    }
    public void setPrerequisiteCourses(List<String> prerequisiteCourses) {
        this.prerequisiteCourses = prerequisiteCourses == null ? new ArrayList<>() : new ArrayList<>(prerequisiteCourses);
    }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public boolean hasSeatsAvailable() {
        return enrolledCount < capacity;
    }

    public int remainingSeats() {
        return Math.max(0, capacity - enrolledCount);
    }
}
