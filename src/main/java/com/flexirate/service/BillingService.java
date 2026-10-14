package com.flexirate.service;

import com.flexirate.dto.BillingRequestDto;
import com.flexirate.dto.BillingResponseDto;

/**
 * Application boundary used by the REST controller; implementations are stateless across requests.
 */
public interface BillingService {
    /**
     * Calculates a complete snapshot and returns mapped user bills in input-user order.
     * Invalid events are counted in response metadata; invalid requests raise a request exception.
     * No usage, grace balance or invoice is persisted.
     */
    BillingResponseDto calculate(BillingRequestDto billingRequestDto);
}
