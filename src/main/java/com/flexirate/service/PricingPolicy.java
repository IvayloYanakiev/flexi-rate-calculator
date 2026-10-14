package com.flexirate.service;

import com.flexirate.domain.Bill;
import com.flexirate.domain.User;
import com.flexirate.domain.UsageSummary;

import java.time.YearMonth;

/**
 * Strategy contract operates on usage, allowing tariffs to change without changing ingestion.
 */
public interface PricingPolicy {
    /**
     * Prices exact seconds into a domain bill whose rounded base and surcharge sum to its total.
     */
    Bill price(User user, YearMonth billingCycle, UsageSummary usageSummary);
}
