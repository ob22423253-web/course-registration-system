package com.uni.registration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated pool for concurrent registration requests. We isolate it from Tomcat's
 * request threads so a registration spike can't starve the HTTP server.
 */
@Configuration
public class ExecutorConfig {

    @Value("${registration.executor.core-pool-size:8}")
    private int corePoolSize;

    @Value("${registration.executor.max-pool-size:16}")
    private int maxPoolSize;

    @Value("${registration.executor.queue-capacity:500}")
    private int queueCapacity;

    @Bean(name = "registrationTaskExecutor")
    public Executor registrationTaskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(corePoolSize);
        ex.setMaxPoolSize(maxPoolSize);
        ex.setQueueCapacity(queueCapacity);
        ex.setThreadNamePrefix("reg-worker-");
        // Caller-runs as backpressure: better to slow down than to silently drop a registration.
        ex.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }
}
