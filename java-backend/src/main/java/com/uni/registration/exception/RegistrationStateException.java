package com.uni.registration.exception;

/**
 * Unchecked counterpart for *programmer errors* — null IDs, illegal state transitions,
 * or missing entities the system invariants say must exist. We do not force callers
 * to wrap try/catch around these because there is no sensible recovery path.
 */
public class RegistrationStateException extends RuntimeException {

    private final String code;

    public RegistrationStateException(String code, String message) {
        super(message);
        this.code = code;
    }

    public RegistrationStateException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() { return code; }
}
