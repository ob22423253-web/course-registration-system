package com.uni.registration.repository;

import com.uni.registration.adt.Repository;
import com.uni.registration.domain.Enrollment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EnrollmentRepositoryAdapter implements Repository<Enrollment, String> {

    private final EnrollmentMongoRepository delegate;

    public EnrollmentRepositoryAdapter(EnrollmentMongoRepository delegate) {
        this.delegate = delegate;
    }

    @Override public Enrollment save(Enrollment e) { return delegate.save(e); }
    @Override public Optional<Enrollment> findById(String id) { return delegate.findById(id); }
    @Override public List<Enrollment> findAll() { return delegate.findAll(); }
    @Override public boolean existsById(String id) { return delegate.existsById(id); }
    @Override public void deleteById(String id) { delegate.deleteById(id); }
    @Override public long count() { return delegate.count(); }

    public List<Enrollment> findByStudentId(String studentId) { return delegate.findByStudentId(studentId); }
    public List<Enrollment> findByCourseId(String courseId) { return delegate.findByCourseId(courseId); }
    public Optional<Enrollment> findByStudentIdAndCourseId(String s, String c) {
        return delegate.findByStudentIdAndCourseId(s, c);
    }
}
