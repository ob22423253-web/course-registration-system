package com.uni.registration.repository;

import com.uni.registration.domain.Course;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CourseMongoRepository extends MongoRepository<Course, String> {
}
