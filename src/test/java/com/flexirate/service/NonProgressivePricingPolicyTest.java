package com.flexirate.service;

import com.flexirate.domain.Bill;
import com.flexirate.domain.Tier;
import com.flexirate.domain.UsageSummary;
import com.flexirate.domain.User;
import com.flexirate.service.impl.NonProgressivePricingPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class NonProgressivePricingPolicyTest {
    private final PricingPolicy pricingPolicy = new NonProgressivePricingPolicy();
    private final YearMonth billingCycle = YearMonth.of(2026, 10);

    @ParameterizedTest
    @CsvSource({"false,99,1,1.00,89.00", "false,100,1,1.00,90.00",
            "false,110,2,0.80,80.00", "false,510,3,0.50,250.00",
            "true,99,1,1.00,89.00", "true,100,2,0.80,72.00", "true,500,3,0.50,245.00"})
    void oneRateAppliesToAllPaidHours(boolean enterprise, long totalHours, int expectedTier,
                                    String expectedHourlyRate, String expectedCost) {
        // Given: the first 10 hours are free for either user type; no paid peak usage here.
        User user = new User("alice", enterprise, ZoneId.of("UTC"));
        UsageSummary usageSummary = new UsageSummary(totalHours * 3600, 10 * 3600, 0);

        // When: regular tier selection excludes free hours; enterprise selection includes them.
        Bill bill = pricingPolicy.price(user, billingCycle, usageSummary);

        // Then: every paid hour uses the selected rate, rather than progressive rate bands.
        Tier tier = bill.tierBreakdown().getFirst();
        assertEquals(expectedTier, tier.tier());
        assertEquals(new BigDecimal(expectedHourlyRate), tier.hourlyRate());
        assertEquals(new BigDecimal(expectedCost), bill.totalCost());
        assertTrue(bill.appliedSurcharges().isEmpty());
    }

    @Test
    void paidPeakQuarterHourAddsFiveCents() {
        // Given: 10h15m total, 10h free and the remaining 15 minutes inside peak hours.
        User user = new User("alice", false, ZoneId.of("Europe/Sofia"));
        UsageSummary usageSummary = new UsageSummary(36_900, 36_000, 900);

        Bill bill = pricingPolicy.price(user, billingCycle, usageSummary);

        // Then: 0.25h * $1 = $0.25 base; 0.25h * $0.20 = $0.05 extra; total $0.30.
        assertEquals(new BigDecimal("0.25"), bill.tierBreakdown().getFirst().cost());
        assertEquals(new BigDecimal("0.05"), bill.appliedSurcharges().getFirst().cost());
        assertEquals(new BigDecimal("0.30"), bill.totalCost());
        assertEquals("alice", bill.userId());
        assertEquals("2026-10", bill.billingCycle());
    }

    @Test
    void separatelyRoundedCostsReconcileWithTotal() {
        // Given: 18 paid peak seconds. Base is $0.005 and the extra charge is $0.001.
        User user = new User("alice", false, ZoneId.of("UTC"));
        UsageSummary usageSummary = new UsageSummary(36_018, 36_000, 18);

        Bill bill = pricingPolicy.price(user, billingCycle, usageSummary);

        // HALF_UP rounds base to $0.01 and surcharge to $0.00; sum those displayed costs.
        BigDecimal baseCost = bill.tierBreakdown().getFirst().cost();
        BigDecimal surchargeCost = bill.appliedSurcharges().getFirst().cost();
        assertEquals(new BigDecimal("0.01"), baseCost);
        assertEquals(new BigDecimal("0.00"), surchargeCost);
        assertEquals(baseCost.add(surchargeCost), bill.totalCost());
    }

    @Test
    void freeOnlyUsageCostsNothing() {
        Bill bill = pricingPolicy.price(new User("alice", false, ZoneId.of("UTC")), billingCycle,
                new UsageSummary(900, 900, 0));

        assertEquals(new BigDecimal("0.00"), bill.totalCost());
        assertEquals(new BigDecimal("0.25"), bill.tierBreakdown().getFirst().freeHours());
        assertEquals(new BigDecimal("0.00"), bill.tierBreakdown().getFirst().billableHours());
        assertTrue(bill.appliedSurcharges().isEmpty());
    }
}
