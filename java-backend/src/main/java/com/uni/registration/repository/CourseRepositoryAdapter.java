package com.uni.registration.repository;

import com.uni.registration.adt.Repository;
import com.uni.registration.config.CacheConfig;
import com.uni.registration.domain.Course;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CourseRepositoryAdapter implements Repository<Course, String> {

    private final CourseMongoRepository delegate;

    public CourseRepositoryAdapter(CourseMongoRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    @CacheEvict(value = CacheConfig.COURSE_CACHE, key = "#course.courseId")
    public Course save(Course course) {
        // Evict on every save — capacity/title may have changed and we never want
        // stale eligibility decisions due to a cached prereq rule.
        return delegate.save(course);
    }

    @Override
    @Cacheable(value = CacheConfig.COURSE_CACHE, key = "#id")
    public Optional<Course> findById(String id) {
        return delegate.findById(id);
    }

    @Override public List<Course> findAll() { return delegate.findAll(); }
    @Override public boolean existsById(String id) { return delegate.existsById(id); }

    @Override
    @CacheEvict(value = CacheConfig.COURSE_CACHE, key = "#id")
    public void deleteById(String id) { delegate.deleteById(id); }

    @Override public long count() { return delegate.count(); }
}
