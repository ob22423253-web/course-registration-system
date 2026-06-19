package com.uni.registration.service;

import com.uni.registration.domain.Student;
import com.uni.registration.exception.StudentNotFoundException;
import com.uni.registration.repository.StudentRepositoryAdapter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepositoryAdapter repo;
    private final PythonAnalyticsClient analytics;

    public StudentService(StudentRepositoryAdapter repo, PythonAnalyticsClient analytics) {
        this.repo = repo;
        this.analytics = analytics;
    }

    public Student create(Student s) { return repo.save(s); }

    public Student findById(String id) {
        return repo.findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    public List<Student> findAll() { return repo.findAll(); }

    /** Asks Python to recompute GPA, persists the new value, returns it. */
    public double refreshGpa(String studentId) {
        Student s = findById(studentId);
        double gpa = analytics.computeGpa(s);
        s.setGpa(gpa);
        repo.save(s);
        return gpa;
    }
}
