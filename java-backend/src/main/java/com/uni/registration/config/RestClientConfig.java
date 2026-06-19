package com.uni.registration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * The Python service is a synchronous dependency for eligibility decisions, so we
 * cap timeouts aggressively — a hung analytics call must not hold a registration thread.
 */
@Configuration
public class RestClientConfig {

    @Value("${python.service.timeout-ms:5000}")
    private long timeoutMs;

    @Bean
    public RestTemplate pythonRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
