package com.flexirate.domain;

import com.flexirate.util.Money;

/**
 * Strategy: the free allowance is identical, but enterprise tier eligibility differs.
 */
public enum GracePolicy {
    REGULAR {
        /** Free seconds do not contribute to a regular user's tariff threshold. */
        @Override
        public long tierEligibleSeconds(UsageSummary usageSummary) {
            return usageSummary.paidSeconds();
        }
    },
    ENTERPRISE {
        /** Enterprise free seconds affect the tariff threshold while remaining unbilled. */
        @Override
        public long tierEligibleSeconds(UsageSummary usageSummary) {
            return usageSummary.totalSeconds();
        }
    };

    public static final long ALLOWANCE_SECONDS = 10 * Money.SECONDS_PER_HOUR;

    /**
     * Chooses the exact usage counter against which tier thresholds are evaluated.
     */
    public abstract long tierEligibleSeconds(UsageSummary usageSummary);

    /**
     * Selects the regular/enterprise strategy without changing the shared ten-hour allowance.
     */
    public static GracePolicy forUser(User user) {
        return user.enterprise() ? ENTERPRISE : REGULAR;
    }
}
