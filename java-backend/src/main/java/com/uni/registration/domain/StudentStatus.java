package com.uni.registration.domain;

/**
 * Status drives eligibility rules — the Python side pattern-matches on this value
 * to apply different validation paths.
 */
public enum StudentStatus {
    ACTIVE,
    PROBATION,
    SUSPENDED,
    GRADUATED
}
