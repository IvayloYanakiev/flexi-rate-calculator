package com.flexirate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Paid peak component; percentage 0.20 means 20 percent, and cost is rounded to cents.
 */
public record SurchargeDto(String type, BigDecimal percentage,
                           @JsonProperty("billable_peak_hours") BigDecimal billablePeakHours, BigDecimal cost) {
}
