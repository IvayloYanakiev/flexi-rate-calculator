package com.flexirate.service.impl;

import com.flexirate.service.PricingPolicy;

import com.flexirate.domain.Bill;
import com.flexirate.domain.Tier;
import com.flexirate.domain.Surcharge;
import com.flexirate.domain.GracePolicy;
import com.flexirate.domain.RateTier;
import com.flexirate.domain.UsageSummary;
import com.flexirate.domain.User;
import com.flexirate.util.Money;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Applies one hourly rate to all paid usage, then adds 20% for paid UTC peak usage.
 */
@Component
public class NonProgressivePricingPolicy implements PricingPolicy {
    private static final BigDecimal PEAK_SURCHARGE_PERCENTAGE = new BigDecimal("0.20");

    /**
     * Selects the user's monthly rate, calculates base and peak costs, and builds the bill.
     * Free usage affects enterprise rate selection but is never charged.
     * Both costs are rounded to cents before being added to the total.
     */
    @Override
    public Bill price(User user, YearMonth billingCycle, UsageSummary usageSummary) {
        // 1. Choose one rate for the month. Regular users exclude free time from the tier threshold;
        // enterprise users include it. For example, 100 total hours means 90 eligible hours for
        // a regular user ($1/hour), but 100 eligible hours for an enterprise user ($0.80/hour).
        GracePolicy gracePolicy = GracePolicy.forUser(user);
        long tierEligibleSeconds = gracePolicy.tierEligibleSeconds(usageSummary);
        RateTier rateTier = RateTier.select(tierEligibleSeconds);
        BigDecimal hourlyRate = rateTier.hourlyRate();

        // 2. Charge all paid usage at that rate. Paid peak time is already included here.
        BigDecimal baseCost = Money.charge(usageSummary.paidSeconds(), hourlyRate);

        // 3. Calculate only the extra 20% for paid peak time: at $1/hour, the extra rate is $0.20/hour.
        BigDecimal peakSurchargeRate = hourlyRate.multiply(PEAK_SURCHARGE_PERCENTAGE);
        BigDecimal surchargeCost = Money.charge(usageSummary.paidPeakSeconds(), peakSurchargeRate);

        // 4. Build the breakdown. Hour values are for display; costs above use exact seconds.
        Tier tierBreakdown = new Tier(
                rateTier.number(),
                hourlyRate,
                Money.hours(tierEligibleSeconds),
                Money.hours(usageSummary.freeSeconds()),
                Money.hours(usageSummary.paidSeconds()),
                baseCost);
        List<Surcharge> appliedSurcharges = List.of();
        if (usageSummary.paidPeakSeconds() > 0) {
            Surcharge peakSurcharge = new Surcharge(
                    "UTC_PEAK",
                    PEAK_SURCHARGE_PERCENTAGE,
                    Money.hours(usageSummary.paidPeakSeconds()),
                    surchargeCost);
            appliedSurcharges = List.of(peakSurcharge);
        }

        // 5. Add the rounded costs so the breakdown matches the total: $0.25 + $0.05 = $0.30.
        BigDecimal totalCost = baseCost.add(surchargeCost);
        return new Bill(user.userId(), billingCycle.toString(), totalCost, List.of(tierBreakdown), appliedSurcharges);
    }
}
