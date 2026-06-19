package com.uni.registration.dto;

public class RegistrationResponse {

    public enum Outcome { ENROLLED, WAITLISTED, REJECTED }

    private Outcome outcome;
    private String enrollmentId;
    private String message;
    private Integer waitlistPosition;

    public RegistrationResponse() { }

    public RegistrationResponse(Outcome outcome, String enrollmentId, String message, Integer waitlistPosition) {
        this.outcome = outcome;
        this.enrollmentId = enrollmentId;
        this.message = message;
        this.waitlistPosition = waitlistPosition;
    }

    public static RegistrationResponse enrolled(String enrollmentId) {
        return new RegistrationResponse(Outcome.ENROLLED, enrollmentId, "Enrolled", null);
    }

    public static RegistrationResponse waitlisted(String enrollmentId, int position) {
        return new RegistrationResponse(Outcome.WAITLISTED, enrollmentId, "Course full — added to waitlist", position);
    }

    public static RegistrationResponse rejected(String reason) {
        return new RegistrationResponse(Outcome.REJECTED, null, reason, null);
    }

    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }
    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Integer getWaitlistPosition() { return waitlistPosition; }
    public void setWaitlistPosition(Integer waitlistPosition) { this.waitlistPosition = waitlistPosition; }
}
