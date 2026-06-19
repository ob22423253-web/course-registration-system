package com.uni.registration.dto;

import java.util.List;

public class EligibilityResponse {

    private boolean eligible;
    private String reason;
    private double gpa;
    private List<String> missingPrerequisites;

    public EligibilityResponse() { }

    public boolean isEligible() { return eligible; }
    public void setEligible(boolean eligible) { this.eligible = eligible; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public double getGpa() { return gpa; }
    public void setGpa(double gpa) { this.gpa = gpa; }
    public List<String> getMissingPrerequisites() { return missingPrerequisites; }
    public void setMissingPrerequisites(List<String> missingPrerequisites) {
        this.missingPrerequisites = missingPrerequisites;
    }
}
