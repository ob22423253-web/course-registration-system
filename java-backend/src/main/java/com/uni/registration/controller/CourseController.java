package com.uni.registration.controller;

import com.uni.registration.domain.Course;
import com.uni.registration.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @PostMapping
    public Course create(@RequestBody Course course) { return service.create(course); }

    @GetMapping("/{id}")
    public Course findById(@PathVariable String id) { return service.findById(id); }

    @GetMapping
    public List<Course> findAll() { return service.findAll(); }

    @GetMapping("/open")
    public List<Course> openCourses() { return service.openCoursesSortedByRemaining(); }
}
