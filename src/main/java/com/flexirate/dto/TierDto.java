package com.flexirate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Selected non-progressive tier; cost is the base component and excludes peak surcharge.
 */
public record TierDto(int tier, @JsonProperty("hourly_rate") BigDecimal hourlyRate,
                      @JsonProperty("eligible_hours") BigDecimal eligibleHours,
                      @JsonProperty("free_hours") BigDecimal freeHours,
                      @JsonProperty("billable_hours") BigDecimal billableHours, BigDecimal cost) {
}
