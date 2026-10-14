package com.flexirate.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pricing result independent of HTTP; total is the sum of rounded component costs.
 */
public record Bill(String userId, String billingCycle, BigDecimal totalCost,
                   List<Tier> tierBreakdown, List<Surcharge> appliedSurcharges) {
}
