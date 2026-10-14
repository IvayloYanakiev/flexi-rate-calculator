package com.flexirate.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Validated startup configuration bounding batch size, request-body size and local cache entries.
 */
@Validated
@ConfigurationProperties("billing")
public record BillingProperties(
        @Min(1) @Max(100000) int maxUsers,
        @Min(1) @Max(1000000) int maxEvents,
        @Min(1024) @Max(67108864) int maxRequestBytes,
        boolean cacheEnabled,
        @Min(1) @Max(100000) int cacheMaximumSize) {
}
