package com.uni.registration.domain;

/**
 * Shared identity for any human in the system. Kept abstract so we never
 * accidentally persist a "Person" — only concrete subtypes (Student today,
 * Instructor tomorrow) live in collections.
 */
public abstract class Person {

    // Fields stay private — all access goes through getters so subclasses (and Mongo
    // mapping) can't bypass invariants.
    private String id;
    private String firstName;
    private String lastName;
    private String email;

    protected Person() { }

    protected Person(String id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String fullName() {
        return firstName + " " + lastName;
    }

    /** Subclasses describe their own role for logging/audit trails. */
    public abstract String role();
}
