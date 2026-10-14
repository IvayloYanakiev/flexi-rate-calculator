package com.flexirate;

import com.flexirate.config.BillingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Boots the MVC calculator and validates its external resource-limit configuration.
 */
@SpringBootApplication
@EnableConfigurationProperties(BillingProperties.class)
public class FlexiRateApplication {
    /**
     * Starts the embedded HTTP server and Spring-managed service implementations.
     */
    public static void main(String[] args) {
        SpringApplication.run(FlexiRateApplication.class, args);
    }
}
