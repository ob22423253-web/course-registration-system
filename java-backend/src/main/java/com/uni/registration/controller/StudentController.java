package com.uni.registration.controller;

import com.uni.registration.domain.Student;
import com.uni.registration.service.StudentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @PostMapping
    public Student create(@RequestBody Student s) { return service.create(s); }

    @GetMapping("/{id}")
    public Student findById(@PathVariable String id) { return service.findById(id); }

    @GetMapping
    public List<Student> findAll() { return service.findAll(); }

    /** Round-trip call into Python that returns the freshly computed GPA. */
    @PostMapping("/{id}/gpa/refresh")
    public Map<String, Object> refreshGpa(@PathVariable String id) {
        double gpa = service.refreshGpa(id);
        return Map.of("studentId", id, "gpa", gpa);
    }
}
