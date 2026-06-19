package com.uni.registration.exception;

/**
 * Base of the *checked* registration-failure family. Made checked deliberately:
 * these are predictable business outcomes that callers must handle (or explicitly
 * declare they're propagating). Programmer bugs use the unchecked sibling
 * {@link RegistrationStateException}.
 */
public class RegistrationException extends Exception {

    private final String code;

    public RegistrationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public RegistrationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() { return code; }
}
