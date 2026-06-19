package com.uni.registration.support;

import com.uni.registration.domain.Student;
import com.uni.registration.repository.StudentMongoRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class InMemoryStudentRepo implements StudentMongoRepository {

    private final Map<String, Student> data = new ConcurrentHashMap<>();

    @Override
    public Optional<Student> findByEmailIndex(String email) {
        return data.values().stream().filter(s -> email.equals(s.getEmailIndex())).findFirst();
    }

    @Override public <S extends Student> S save(S s) { data.put(s.getStudentId(), s); return s; }
    @Override public <S extends Student> List<S> saveAll(Iterable<S> es) {
        List<S> out = new ArrayList<>(); es.forEach(e -> { save(e); out.add(e); }); return out;
    }
    @Override public Optional<Student> findById(String id) { return Optional.ofNullable(data.get(id)); }
    @Override public boolean existsById(String id) { return data.containsKey(id); }
    @Override public List<Student> findAll() { return new ArrayList<>(data.values()); }
    @Override public List<Student> findAllById(Iterable<String> ids) {
        List<Student> out = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(out::add)); return out;
    }
    @Override public long count() { return data.size(); }
    @Override public void deleteById(String id) { data.remove(id); }
    @Override public void delete(Student e) { data.remove(e.getStudentId()); }
    @Override public void deleteAllById(Iterable<? extends String> ids) { ids.forEach(data::remove); }
    @Override public void deleteAll(Iterable<? extends Student> es) { es.forEach(this::delete); }
    @Override public void deleteAll() { data.clear(); }

    @Override public <S extends Student> S insert(S entity) { return save(entity); }
    @Override public <S extends Student> List<S> insert(Iterable<S> es) { return saveAll(es); }
    @Override public List<Student> findAll(Sort sort) { return findAll(); }
    @Override public Page<Student> findAll(Pageable p) { return new PageImpl<>(findAll()); }
    @Override public <S extends Student> Optional<S> findOne(Example<S> e) { throw new UnsupportedOperationException(); }
    @Override public <S extends Student> List<S> findAll(Example<S> e) { throw new UnsupportedOperationException(); }
    @Override public <S extends Student> List<S> findAll(Example<S> e, Sort s) { throw new UnsupportedOperationException(); }
    @Override public <S extends Student> Page<S> findAll(Example<S> e, Pageable p) { throw new UnsupportedOperationException(); }
    @Override public <S extends Student> long count(Example<S> e) { throw new UnsupportedOperationException(); }
    @Override public <S extends Student> boolean exists(Example<S> e) { throw new UnsupportedOperationException(); }
    @Override public <S extends Student, R> R findBy(Example<S> e, Function<FluentQuery.FetchableFluentQuery<S>, R> q) { throw new UnsupportedOperationException(); }
}
