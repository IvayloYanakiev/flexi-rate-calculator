package com.flexirate.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies an injectable clock so calculations capture a consistent instant and tests can fix time.
 */
@Configuration
public class ClockConfiguration {
    /**
     * Uses UTC for the clock; the time service derives billing months in each user's timezone.
     */
    @Bean
    public Clock billingClock() {
        return Clock.systemUTC();
    }
}
