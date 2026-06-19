package com.uni.registration.adt;

import java.util.List;
import java.util.Optional;

/**
 * Our own generic Repository contract — kept independent of Spring Data so services
 * depend on this interface, not MongoRepository. That lets us swap persistence (or
 * stub it in tests) without touching service code.
 *
 * @param <T>  entity type
 * @param <ID> identifier type
 */
public interface Repository<T, ID> {

    /** Persist new or update existing — returns the saved aggregate (with assigned id). */
    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    boolean existsById(ID id);

    void deleteById(ID id);

    long count();
}
