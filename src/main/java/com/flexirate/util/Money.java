package com.flexirate.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts exact seconds to displayed hours and USD components without binary floating-point arithmetic.
 */
public final class Money {
    public static final long SECONDS_PER_HOUR = 3_600L;

    /**
     * Prevents instances of this stateless arithmetic utility.
     */
    private Money() {
    }

    /**
     * Produces informational hour values at scale 2 using HALF_UP; charges use exact seconds instead of these displays.
     */
    public static BigDecimal hours(long durationSeconds) {
        return BigDecimal.valueOf(durationSeconds).divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 2, RoundingMode.HALF_UP);
    }

    /**
     * Multiplies seconds by an hourly rate and rounds the component once to cents using HALF_UP.
     */
    public static BigDecimal charge(long durationSeconds, BigDecimal hourlyRate) {
        return BigDecimal.valueOf(durationSeconds).multiply(hourlyRate)
                .divide(BigDecimal.valueOf(SECONDS_PER_HOUR), 2, RoundingMode.HALF_UP);
    }
}
