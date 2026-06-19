package com.uni.registration.repository;

import com.uni.registration.adt.Repository;
import com.uni.registration.domain.Student;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adapts Spring Data's repository onto our own Repository&lt;T, ID&gt; contract so
 * services depend on the abstraction we own.
 */
@Component
public class StudentRepositoryAdapter implements Repository<Student, String> {

    private final StudentMongoRepository delegate;

    public StudentRepositoryAdapter(StudentMongoRepository delegate) {
        this.delegate = delegate;
    }

    @Override public Student save(Student s) { return delegate.save(s); }
    @Override public Optional<Student> findById(String id) { return delegate.findById(id); }
    @Override public List<Student> findAll() { return delegate.findAll(); }
    @Override public boolean existsById(String id) { return delegate.existsById(id); }
    @Override public void deleteById(String id) { delegate.deleteById(id); }
    @Override public long count() { return delegate.count(); }

    public Optional<Student> findByEmail(String email) { return delegate.findByEmailIndex(email); }
}
