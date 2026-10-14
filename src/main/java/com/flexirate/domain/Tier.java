package com.flexirate.domain;

import java.math.BigDecimal;

/**
 * One selected whole-month tariff and its base charge, with displayed grace/eligible/paid hours.
 */
public record Tier(int tier, BigDecimal hourlyRate, BigDecimal eligibleHours,
                   BigDecimal freeHours, BigDecimal billableHours, BigDecimal cost) {
}
