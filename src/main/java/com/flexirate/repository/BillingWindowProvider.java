package com.flexirate.repository;

import com.flexirate.domain.BillingWindow;

import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Provides derived calendar windows without implying persistent user or invoice storage.
 */
public interface BillingWindowProvider {
    /**
     * Returns immutable local-month boundaries; caching must not change the calculated result.
     */
    BillingWindow find(ZoneId timezone, YearMonth billingCycle);
}
