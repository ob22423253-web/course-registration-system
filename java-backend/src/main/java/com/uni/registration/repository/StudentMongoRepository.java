package com.uni.registration.repository;

import com.uni.registration.domain.Student;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StudentMongoRepository extends MongoRepository<Student, String> {
    Optional<Student> findByEmailIndex(String email);
}
