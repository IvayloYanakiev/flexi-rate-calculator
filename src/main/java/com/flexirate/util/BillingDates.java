package com.flexirate.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Pure conversions used by generated MapStruct mappings and request validation.
 */
public final class BillingDates {
    /**
     * Prevents instances of this stateless conversion utility.
     */
    private BillingDates() {
    }

    /**
     * Parses an IANA timezone or supported fixed offset; invalid values raise DateTimeException.
     */
    public static ZoneId timezone(String timezone) {
        return ZoneId.of(timezone);
    }

    /**
     * Requires an offset-bearing timestamp and normalizes it to an absolute instant.
     */
    public static Instant timestamp(String timestamp) {
        return OffsetDateTime.parse(timestamp).toInstant();
    }

    /**
     * Parses a year/month after request validation has checked the API's strict YYYY-MM shape.
     */
    public static YearMonth cycle(String billingCycle) {
        return YearMonth.parse(billingCycle);
    }

    /**
     * Resolves the earliest valid local time on day one, including timezone transitions at midnight.
     */
    public static Instant monthStart(ZoneId timezone, YearMonth billingCycle) {
        return billingCycle.atDay(1).atStartOfDay(timezone).toInstant();
    }

    /**
     * Uses next month's start as the exclusive boundary, avoiding fixed-length-month assumptions.
     */
    public static Instant monthEnd(ZoneId timezone, YearMonth billingCycle) {
        return monthStart(timezone, billingCycle.plusMonths(1));
    }

    /**
     * Adds the exact 900-second duration of an accepted usage chunk.
     */
    public static Instant chunkEnd(Instant chunkStart) {
        return chunkStart.plusSeconds(15 * 60);
    }
}
