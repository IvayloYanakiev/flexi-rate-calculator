package com.flexirate.domain;

import java.time.Instant;

import com.flexirate.util.BillingDates;

/**
 * A validated, normalized 15-minute chunk; user ownership belongs to its billing bucket.
 */
public record UsageChunk(String eventId, Instant timestamp) {
    /**
     * Returns the exclusive end of this fixed 15-minute chunk.
     */
    public Instant end() {
        return BillingDates.chunkEnd(timestamp);
    }
}
