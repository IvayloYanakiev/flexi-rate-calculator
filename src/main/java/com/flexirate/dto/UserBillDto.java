package com.flexirate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * One user's API bill, including the selected local month and reconciling base/surcharge components.
 */
public record UserBillDto(
        @JsonProperty("user_id") String userId,
        @JsonProperty("billing_cycle") String billingCycle,
        @JsonProperty("total_cost") BigDecimal totalCost,
        @JsonProperty("tier_breakdown") List<TierDto> tierBreakdown,
        @JsonProperty("applied_surcharges") List<SurchargeDto> appliedSurcharges) {
}
