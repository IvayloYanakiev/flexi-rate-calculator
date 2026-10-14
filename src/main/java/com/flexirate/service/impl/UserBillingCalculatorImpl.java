package com.flexirate.service.impl;

import com.flexirate.service.UserBillingCalculator;
import com.flexirate.service.BillingTimeService;
import com.flexirate.service.PricingPolicy;
import com.flexirate.domain.UsageAccumulator;
import com.flexirate.domain.UsageSummary;

import com.flexirate.domain.Bill;
import com.flexirate.domain.UsageChunk;
import com.flexirate.domain.User;
import com.flexirate.domain.BillingWindow;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Sorts a request-owned user's chunks, accumulates usage, then delegates the completed summary to pricing.
 */
@Component
public class UserBillingCalculatorImpl implements UserBillingCalculator {
    private static final Comparator<UsageChunk> CHRONOLOGICAL =
            Comparator.comparing(UsageChunk::timestamp).thenComparing(UsageChunk::eventId);
    private final BillingTimeService billingTimeService;
    private final PricingPolicy pricingPolicy;

    /**
     * Combines chronological time accounting with a replaceable tariff strategy.
     */
    public UserBillingCalculatorImpl(BillingTimeService billingTimeService, PricingPolicy pricingPolicy) {
        this.billingTimeService = billingTimeService;
        this.pricingPolicy = pricingPolicy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bill calculate(User user, YearMonth billingCycle, BillingWindow billingWindow, List<UsageChunk> usageChunks) {
        // Collect total, free and paid peak seconds before calculating monetary charges.
        UsageSummary usageSummary = summarizeUsage(usageChunks, billingWindow);
        return pricingPolicy.price(user, billingCycle, usageSummary);
    }

    /**
     * Processes chunks chronologically so the earliest usage consumes the free allowance first.
     * The time service clips each chunk to the billing month and counts paid peak usage.
     * For example, if only 15 free minutes remain, 08:45-09:00 UTC is free and
     * 09:00-09:15 UTC is paid peak usage, even if the request lists the 09:00 event first.
     *
     * @param usageChunks this user's request-owned mutable list; sorted in place by timestamp, then ID
     * @param billingWindow inclusive start and exclusive end of the user's local billing month
     * @return immutable total, free and paid peak durations in exact seconds, ready for pricing
     */
    private UsageSummary summarizeUsage(List<UsageChunk> usageChunks, BillingWindow billingWindow) {
        // Process earliest events first so the first 10 hours are free; event ID breaks timestamp ties.
        // This list belongs only to this user/request, so sorting it does not affect other requests.
        usageChunks.sort(CHRONOLOGICAL);
        // Start fresh total/free/paid-peak counters for this user; no allowance carries across requests.
        UsageAccumulator usageAccumulator = new UsageAccumulator();
        // Visit every accepted chunk in chronological order.
        for (UsageChunk usageChunk : usageChunks) {
            // Count only the part inside the month, consume remaining free seconds, then count paid
            // seconds within 09:00-17:00 UTC. A full chunk adds 900 total seconds before any clipping.
            billingTimeService.accumulateUsage(usageChunk, billingWindow, usageAccumulator);
        }
        // Copy the counters into an immutable UsageSummary; the pricing policy calculates money next.
        return usageAccumulator.summary();
    }
}
