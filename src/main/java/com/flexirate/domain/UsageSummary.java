package com.flexirate.domain;

/**
 * Exact whole-second counters, independent of JSON and monetary presentation.
 */
public record UsageSummary(long totalSeconds, long freeSeconds, long paidPeakSeconds) {
    /**
     * Excludes the allocated free allowance from billable usage.
     */
    public long paidSeconds() {
        return totalSeconds - freeSeconds;
    }
}
