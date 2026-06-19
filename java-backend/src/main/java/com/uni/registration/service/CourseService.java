package com.uni.registration.service;

import com.uni.registration.domain.Course;
import com.uni.registration.exception.CourseNotFoundException;
import com.uni.registration.repository.CourseRepositoryAdapter;
import com.uni.registration.util.Streams;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@Service
public class CourseService {

    private final CourseRepositoryAdapter repo;

    public CourseService(CourseRepositoryAdapter repo) {
        this.repo = repo;
    }

    public Course create(Course course) {
        return repo.save(course);
    }

    public Course findById(String id) {
        return repo.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
    }

    public List<Course> findAll() { return repo.findAll(); }

    /** Demonstrates Predicate + Comparator + streams in one shot. */
    public List<Course> openCoursesSortedByRemaining() {
        Predicate<Course> hasSeats = Course::hasSeatsAvailable;
        Comparator<Course> byRemaining = Comparator.comparingInt(Course::remainingSeats).reversed();
        return Streams.sorted(Streams.filterMap(repo.findAll(), hasSeats, c -> c), byRemaining);
    }
}
