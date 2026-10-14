package com.flexirate.domain;

import java.time.Instant;

/**
 * Half-open UTC boundaries of a user's local billing month.
 */
public record BillingWindow(Instant start, Instant end) {
}
