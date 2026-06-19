package com.uni.registration.functional;

import com.uni.registration.adt.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationValidatorTest {

    @Test
    void andThen_short_circuits_on_first_failure() {
        RegistrationValidator first = (s, c) -> Result.success(null);
        RegistrationValidator second = (s, c) -> Result.failure("X", "bad");
        RegistrationValidator third = (s, c) -> { throw new AssertionError("must not run"); };

        Result<Void> r = first.andThen(second).andThen(third).validate(null, null);
        assertFalse(r.isSuccess());
        assertEquals("X", r.errorCode());
    }

    @Test
    void andThen_returns_last_success_when_all_pass() {
        RegistrationValidator a = (s, c) -> Result.success(null);
        RegistrationValidator b = (s, c) -> Result.success(null);
        Result<Void> r = a.andThen(b).validate(null, null);
        assertTrue(r.isSuccess());
    }
}
