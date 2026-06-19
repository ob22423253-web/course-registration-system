package com.uni.registration.service;

import com.uni.registration.domain.Student;
import com.uni.registration.dto.EligibilityRequest;
import com.uni.registration.dto.EligibilityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin REST client that delegates eligibility/GPA work to the Python service.
 * Centralised so retry / fallback logic stays in one place.
 */
@Component
public class PythonAnalyticsClient {

    private static final Logger log = LoggerFactory.getLogger(PythonAnalyticsClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PythonAnalyticsClient(RestTemplate pythonRestTemplate,
                                 @Value("${python.service.url}") String baseUrl) {
        this.restTemplate = pythonRestTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public EligibilityResponse checkEligibility(EligibilityRequest req) {
        String url = baseUrl + "/api/v1/eligibility";
        log.debug("POST {} for student {} course {}", url, req.getStudentId(), req.getCourseId());
        try {
            return restTemplate.postForObject(url, req, EligibilityResponse.class);
        } catch (RestClientException ex) {
            // Fail open-conservatively: deny the registration rather than approve on error.
            // Better to make a student retry than to silently bypass prereq enforcement.
            log.warn("python eligibility call failed: {}", ex.getMessage());
            EligibilityResponse fallback = new EligibilityResponse();
            fallback.setEligible(false);
            fallback.setReason("analytics service unavailable");
            fallback.setGpa(0.0);
            return fallback;
        }
    }

    public double computeGpa(Student student) {
        String url = baseUrl + "/api/v1/gpa";
        try {
            GpaPayload payload = new GpaPayload();
            payload.studentId = student.getStudentId();
            payload.completedCourses = student.getCompletedCourses();
            GpaResult result = restTemplate.postForObject(url, payload, GpaResult.class);
            return result == null ? 0.0 : result.gpa;
        } catch (RestClientException ex) {
            log.warn("python GPA call failed: {}", ex.getMessage());
            return student.getGpa();
        }
    }

    /** Inline payload classes — never reused elsewhere, so they live here. */
    public static class GpaPayload {
        public String studentId;
        public java.util.List<String> completedCourses;
        public String getStudentId() { return studentId; }
        public java.util.List<String> getCompletedCourses() { return completedCourses; }
    }
    public static class GpaResult {
        public double gpa;
        public double getGpa() { return gpa; }
        public void setGpa(double gpa) { this.gpa = gpa; }
    }
}
