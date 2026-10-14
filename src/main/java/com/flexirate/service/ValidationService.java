package com.flexirate.service;

import com.flexirate.domain.UsageChunk;
import com.flexirate.domain.User;
import com.flexirate.dto.EventDto;
import com.flexirate.dto.BillingRequestDto;

import java.time.YearMonth;
import java.util.List;

/**
 * Separates request failures from recoverable individual-event failures.
 */
public interface ValidationService {
    /**
     * Checks batch limits and cycle syntax/range; returns null when each user's current cycle is requested.
     */
    YearMonth validateRequest(BillingRequestDto billingRequestDto);

    /**
     * Bulk-maps users and rejects invalid timezones or duplicate IDs while preserving their order.
     */
    List<User> validateUsers(BillingRequestDto billingRequestDto);

    /**
     * Returns a normalized 15-minute UTC-aligned chunk, or null for a missing/type-invalid/value-invalid event.
     */
    UsageChunk validateChunk(EventDto eventDto);

}
