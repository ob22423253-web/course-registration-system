package com.uni.registration.support;

import com.uni.registration.domain.Enrollment;
import com.uni.registration.repository.EnrollmentMongoRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Hand-written in-memory stand-in for the Mongo-backed enrollment repo. Lets
 * service tests run without an actual database. Implements just enough of
 * MongoRepository for our service code paths.
 */
public class InMemoryEnrollmentRepo implements EnrollmentMongoRepository {

    private final Map<String, Enrollment> data = new ConcurrentHashMap<>();

    @Override
    public List<Enrollment> findByStudentId(String studentId) {
        return data.values().stream().filter(e -> studentId.equals(e.getStudentId())).toList();
    }

    @Override
    public List<Enrollment> findByCourseId(String courseId) {
        return data.values().stream().filter(e -> courseId.equals(e.getCourseId())).toList();
    }

    @Override
    public Optional<Enrollment> findByStudentIdAndCourseId(String s, String c) {
        return data.values().stream()
                .filter(e -> s.equals(e.getStudentId()) && c.equals(e.getCourseId()))
                .findFirst();
    }

    @Override public <S extends Enrollment> S save(S e) { data.put(e.getEnrollmentId(), e); return e; }
    @Override public <S extends Enrollment> List<S> saveAll(Iterable<S> es) {
        List<S> out = new ArrayList<>();
        es.forEach(e -> { save(e); out.add(e); });
        return out;
    }
    @Override public Optional<Enrollment> findById(String id) { return Optional.ofNullable(data.get(id)); }
    @Override public boolean existsById(String id) { return data.containsKey(id); }
    @Override public List<Enrollment> findAll() { return new ArrayList<>(data.values()); }
    @Override public List<Enrollment> findAllById(Iterable<String> ids) {
        List<Enrollment> out = new ArrayList<>();
        ids.forEach(id -> findById(id).ifPresent(out::add));
        return out;
    }
    @Override public long count() { return data.size(); }
    @Override public void deleteById(String id) { data.remove(id); }
    @Override public void delete(Enrollment e) { data.remove(e.getEnrollmentId()); }
    @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(data::remove); }
    @Override public void deleteAll(Iterable<? extends Enrollment> es) { es.forEach(this::delete); }
    @Override public void deleteAll() { data.clear(); }

    // The rest of the MongoRepository contract — not used by our tests, but
    // required to satisfy the interface. Throw to fail loudly if exercised.
    @Override public <S extends Enrollment> S insert(S entity) { return save(entity); }
    @Override public <S extends Enrollment> List<S> insert(Iterable<S> es) { return saveAll(es); }
    @Override public List<Enrollment> findAll(Sort sort) { return findAll(); }
    @Override public Page<Enrollment> findAll(Pageable pageable) { return new PageImpl<>(findAll()); }
    @Override public <S extends Enrollment> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Enrollment> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Enrollment> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
    @Override public <S extends Enrollment> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
    @Override public <S extends Enrollment> long count(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Enrollment> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
    @Override public <S extends Enrollment, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
}
