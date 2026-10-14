package com.flexirate.domain;


/**
 * Mutable aggregate owned by one user's calculation; callers add clipped durations chronologically.
 */
public final class UsageAccumulator {
    private long totalSeconds;
    private long freeSeconds;
    private long paidPeakSeconds;

    /**
     * Adds an in-month duration and returns how many seconds consume the remaining free allowance.
     */
    public long acceptDuration(long durationSeconds) {
        long remainingFreeSeconds = GracePolicy.ALLOWANCE_SECONDS - freeSeconds;
        long allocatedFreeSeconds = Math.min(durationSeconds, remainingFreeSeconds);
        totalSeconds += durationSeconds;
        freeSeconds += allocatedFreeSeconds;
        return allocatedFreeSeconds;
    }

    /**
     * Records peak seconds only after free usage has been excluded.
     */
    public void addPaidPeak(long paidPeakDurationSeconds) {
        paidPeakSeconds += paidPeakDurationSeconds;
    }

    /**
     * Returns an immutable snapshot for pricing after all chronological chunks have been processed.
     */
    public UsageSummary summary() {
        return new UsageSummary(totalSeconds, freeSeconds, paidPeakSeconds);
    }
}
