package com.flexirate.service;

import com.flexirate.domain.Bill;
import com.flexirate.domain.BillingWindow;
import com.flexirate.domain.UsageChunk;
import com.flexirate.domain.User;

import java.time.YearMonth;
import java.util.List;

/**
 * Per-user calculation contract, independent of HTTP DTOs and request parsing.
 */
public interface UserBillingCalculator {
    /**
     * Sorts the request-owned mutable chunk list by timestamp/ID, accounts for grace and peaks,
     * and delegates pricing to the configured tariff. The list must not be shared across requests.
     */
    Bill calculate(User user, YearMonth billingCycle, BillingWindow billingWindow, List<UsageChunk> usageChunks);
}
