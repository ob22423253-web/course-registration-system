package com.uni.registration.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Course catalog rarely changes within a semester, but seat counts change constantly.
 * We cache course metadata (name, prereqs, capacity), not enrollment state. TTL is short
 * so changes in the admin panel propagate quickly.
 */
@Configuration
public class CacheConfig {

    public static final String COURSE_CACHE = "courses";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager(COURSE_CACHE);
        mgr.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .recordStats());
        return mgr;
    }
}
