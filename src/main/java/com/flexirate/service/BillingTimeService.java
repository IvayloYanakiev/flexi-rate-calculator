package com.flexirate.service;

import com.flexirate.domain.BillingWindow;
import com.flexirate.domain.UsageAccumulator;
import com.flexirate.domain.UsageChunk;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Date/time boundary: local billing months, UTC quarter-hour validation and whole-second accounting.
 */
public interface BillingTimeService {
    /**
     * Captures one instant that the facade reuses for every user's current-cycle resolution.
     */
    Instant snapshotTime();

    /**
     * Uses the explicit cycle, or derives a month from the captured instant in the user's timezone.
     */
    YearMonth getBillingCycle(YearMonth explicitBillingCycle, Instant snapshotTime, ZoneId timezone);

    /**
     * Returns the half-open UTC boundaries of the requested local month, respecting timezone rules.
     */
    BillingWindow getMonthWindow(ZoneId timezone, YearMonth billingCycle);

    /**
     * Requires UTC years 0001..9998 and exact :00/:15/:30/:45 starts with no seconds/fractional seconds.
     */
    boolean isValidChunkStart(Instant timestamp);

    /**
     * Tests whether any part of a chunk belongs to the half-open billing window.
     */
    boolean overlaps(UsageChunk usageChunk, BillingWindow billingWindow);

    /**
     * Clips a chunk to the month, allocates grace chronologically, then records paid UTC peak seconds.
     */
    void accumulateUsage(UsageChunk usageChunk, BillingWindow billingWindow, UsageAccumulator usageAccumulator);

    /**
     * Counts seconds intersecting daily [09:00,17:00) UTC peak windows for an ordered paid interval.
     */
    long peakSeconds(Instant intervalStart, Instant intervalEnd);
}
