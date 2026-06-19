package com.uni.registration.integration;

import com.uni.registration.dto.EligibilityRequest;
import com.uni.registration.dto.EligibilityResponse;
import com.uni.registration.service.PythonAnalyticsClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Round-trip integration: PythonAnalyticsClient -> mocked Python endpoint
 * returning the JSON shape FastAPI would. Run with -Dtest=*IntegrationTest.
 *
 * To run against the *real* Python service, point PYTHON_SERVICE_URL at it
 * and exercise the controller layer instead.
 */
class PythonRoundTripIntegrationTest {

    @Test
    void java_python_eligibility_round_trip() {
        RestTemplate rt = new RestTemplateBuilder().build();
        MockRestServiceServer mock = MockRestServiceServer.bindTo(rt).build();

        String json = """
                { "eligible": true, "reason": "ok", "gpa": 3.7, "missingPrerequisites": [] }
                """;
        mock.expect(requestTo("http://fake-python/api/v1/eligibility"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        PythonAnalyticsClient client = new PythonAnalyticsClient(rt, "http://fake-python");
        EligibilityResponse r = client.checkEligibility(new EligibilityRequest(
                "s1", "ACTIVE", 3.7, List.of("CS101"), "CS201", "CS101"));

        assertTrue(r.isEligible());
        assertEquals(3.7, r.getGpa(), 1e-9);
        mock.verify();
    }

    @Test
    void java_python_eligibility_failure_returns_fallback_deny() {
        // Server unreachable -> we must fail closed (deny), never auto-approve.
        RestTemplate rt = new RestTemplateBuilder().build();
        PythonAnalyticsClient client = new PythonAnalyticsClient(rt, "http://localhost:1");
        EligibilityResponse r = client.checkEligibility(new EligibilityRequest(
                "s1", "ACTIVE", 3.7, List.of("CS101"), "CS201", "CS101"));
        assertFalse(r.isEligible());
    }
}
