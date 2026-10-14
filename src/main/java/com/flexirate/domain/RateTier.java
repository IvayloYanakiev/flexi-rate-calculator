package com.flexirate.domain;

import com.flexirate.util.Money;
import java.math.BigDecimal;

/** Immutable tariff values; thresholds are lower-inclusive. */
public enum RateTier {
    FIRST(1, "1.00"),
    SECOND(2, "0.80"),
    THIRD(3, "0.50");

    private final int tierNumber;
    private final BigDecimal hourlyRate;

    /** Creates immutable tariffs from decimal strings so prices are exact. */
    RateTier(int tierNumber, String hourlyRate) {
        this.tierNumber = tierNumber;
        this.hourlyRate = new BigDecimal(hourlyRate);
    }

    /** Returns the stable tier identifier used in the API breakdown. */
    public int number() {
        return tierNumber;
    }
    /** Returns the single hourly rate applied to all paid seconds in the month. */
    public BigDecimal hourlyRate() {
        return hourlyRate;
    }

    /** Applies lower-inclusive 100/500-hour thresholds to exact eligible seconds. */
    public static RateTier select(long eligibleSeconds) {
        if (eligibleSeconds >= 500 * Money.SECONDS_PER_HOUR) {
            return THIRD;
        }
        if (eligibleSeconds >= 100 * Money.SECONDS_PER_HOUR) {
            return SECOND;
        }
        return FIRST;
    }
}
