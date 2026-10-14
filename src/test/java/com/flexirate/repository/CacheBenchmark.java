package com.flexirate.repository;

import com.flexirate.config.BillingProperties;

import java.time.*;

/**
 * Optional local microbenchmark; no timing assertions and no execution during normal tests.
 */
public class CacheBenchmark {
    private static volatile Object sink;

    public static void main(String[] args) {
        BillingWindowRepository cached = new BillingWindowRepository(new BillingProperties(1, 1, 1024, true, 1024));
        BillingWindowRepository uncached = new BillingWindowRepository(new BillingProperties(1, 1, 1024, false, 1024));
        ZoneId zone = ZoneId.of("Europe/Sofia");
        YearMonth cycle = YearMonth.of(2026, 10);
        for (int i = 0; i < 100000; i++) {
            sink = cached.find(zone, cycle);
            sink = uncached.find(zone, cycle);
        }
        int n = 1000000;
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            sink = uncached.find(zone, cycle);
        }
        long plain = System.nanoTime() - start;
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            sink = cached.find(zone, cycle);
        }
        long cache = System.nanoTime() - start;
        System.out.printf("Repeated calendar lookup: uncached %.1f ns/op; Caffeine %.1f ns/op; speedup %.2fx%n",
                (double) plain / n, (double) cache / n, (double) plain / cache);
    }
}
