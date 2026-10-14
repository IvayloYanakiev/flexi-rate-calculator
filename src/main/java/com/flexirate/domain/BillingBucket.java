package com.flexirate.domain;

import java.time.YearMonth;
import java.util.List;

/**
 * Request-local user context; its chunk list is mutable for collection and chronological sorting.
 */
public record BillingBucket(User user, YearMonth billingCycle, BillingWindow billingWindow,
                            List<UsageChunk> usageChunks) {
}
