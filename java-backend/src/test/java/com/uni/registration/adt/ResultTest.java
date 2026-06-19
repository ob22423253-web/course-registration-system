package com.uni.registration.adt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_map_chains() {
        Result<Integer> r = Result.<Integer>success(2).map(x -> x * 3);
        assertTrue(r.isSuccess());
        assertEquals(6, r.get());
    }

    @Test
    void failure_short_circuits_map_and_flatmap() {
        Result<Integer> f = Result.failure("E", "boom");
        Result<Integer> mapped = f.map(x -> x * 2);
        assertFalse(mapped.isSuccess());
        assertEquals("E", mapped.errorCode());

        Result<Integer> fm = f.flatMap(x -> Result.success(x + 1));
        assertFalse(fm.isSuccess());
        assertEquals("boom", fm.errorMessage());
    }

    @Test
    void get_on_failure_throws() {
        assertThrows(RuntimeException.class, () -> Result.failure("E", "x").get());
    }
}
