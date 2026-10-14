package com.flexirate.repository;

import com.flexirate.config.BillingProperties;
import com.flexirate.domain.BillingWindow;
import com.flexirate.util.BillingDates;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.*;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Repository;

/**
 * Derived calendar metadata only; this repository never stores user usage or bills.
 */
@Repository
public class BillingWindowRepository implements BillingWindowProvider {
    public record Key(ZoneId timezone, YearMonth billingCycle) {
    }

    private final Cache<Key, BillingWindow> billingWindowCache;
    private final boolean cacheEnabled;

    /**
     * Builds a bounded local cache; disabling it computes the same immutable window on every lookup.
     */
    public BillingWindowRepository(BillingProperties billingProperties) {
        cacheEnabled = billingProperties.cacheEnabled();
        billingWindowCache = Caffeine.newBuilder().maximumSize(billingProperties.cacheMaximumSize())
                .expireAfterAccess(1, TimeUnit.HOURS).recordStats().build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BillingWindow find(ZoneId timezone, YearMonth billingCycle) {
        Key billingWindowKey = new Key(timezone, billingCycle);
        return cacheEnabled ? billingWindowCache.get(billingWindowKey, BillingWindowRepository::compute) : compute(billingWindowKey);
    }

    /**
     * Resolves local month start/end through timezone rules; used for cache misses and uncached lookups.
     */
    public static BillingWindow compute(Key billingWindowKey) {
        return new BillingWindow(BillingDates.monthStart(billingWindowKey.timezone(), billingWindowKey.billingCycle()),
                BillingDates.monthEnd(billingWindowKey.timezone(), billingWindowKey.billingCycle()));
    }
}
