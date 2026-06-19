package com.uni.registration.support;

import com.uni.registration.domain.Course;
import com.uni.registration.repository.CourseMongoRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class InMemoryCourseRepo implements CourseMongoRepository {

    private final Map<String, Course> data = new ConcurrentHashMap<>();

    @Override public <S extends Course> S save(S c) { data.put(c.getCourseId(), c); return c; }
    @Override public <S extends Course> List<S> saveAll(Iterable<S> cs) {
        List<S> out = new ArrayList<>(); cs.forEach(c -> { save(c); out.add(c); }); return out;
    }
    @Override public Optional<Course> findById(String id) { return Optional.ofNullable(data.get(id)); }
    @Override public boolean existsById(String id) { return data.containsKey(id); }
    @Override public List<Course> findAll() { return new ArrayList<>(data.values()); }
    @Override public List<Course> findAllById(Iterable<String> ids) {
        List<Course> out = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(out::add)); return out;
    }
    @Override public long count() { return data.size(); }
    @Override public void deleteById(String id) { data.remove(id); }
    @Override public void delete(Course e) { data.remove(e.getCourseId()); }
    @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(data::remove); }
    @Override public void deleteAll(Iterable<? extends Course> es) { es.forEach(this::delete); }
    @Override public void deleteAll() { data.clear(); }

    @Override public <S extends Course> S insert(S entity) { return save(entity); }
    @Override public <S extends Course> List<S> insert(Iterable<S> es) { return saveAll(es); }
    @Override public List<Course> findAll(Sort sort) { return findAll(); }
    @Override public Page<Course> findAll(Pageable p) { return new PageImpl<>(findAll()); }
    @Override public <S extends Course> Optional<S> findOne(Example<S> e) { throw new UnsupportedOperationException(); }
    @Override public <S extends Course> List<S> findAll(Example<S> e) { throw new UnsupportedOperationException(); }
    @Override public <S extends Course> List<S> findAll(Example<S> e, Sort s) { throw new UnsupportedOperationException(); }
    @Override public <S extends Course> Page<S> findAll(Example<S> e, Pageable p) { throw new UnsupportedOperationException(); }
    @Override public <S extends Course> long count(Example<S> e) { throw new UnsupportedOperationException(); }
    @Override public <S extends Course> boolean exists(Example<S> e) { throw new UnsupportedOperationException(); }
    @Override public <S extends Course, R> R findBy(Example<S> e, Function<FluentQuery.FetchableFluentQuery<S>, R> q) { throw new UnsupportedOperationException(); }
}
