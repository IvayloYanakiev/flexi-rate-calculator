package com.flexirate.service;

import com.flexirate.domain.BillingWindow;
import com.flexirate.domain.UsageAccumulator;
import com.flexirate.domain.UsageChunk;
import com.flexirate.domain.UsageSummary;
import com.flexirate.repository.BillingWindowProvider;
import com.flexirate.service.impl.BillingTimeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillingTimeServiceTest {
    private final Instant snapshotTime = Instant.parse("2026-10-01T00:30:00Z");
    private final BillingWindowProvider billingWindowProvider = mock(BillingWindowProvider.class);
    private final BillingTimeService billingTimeService = new BillingTimeServiceImpl(
            Clock.fixed(snapshotTime, ZoneOffset.UTC), billingWindowProvider);
    private final BillingWindow billingWindow = new BillingWindow(
            Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"));

    @Test
    void snapshotUsesInjectedClock() {
        assertEquals(snapshotTime, billingTimeService.snapshotTime());
    }

    @Test
    void explicitCycleWinsOverCurrentMonth() {
        YearMonth billingCycle = YearMonth.of(2025, 2);

        assertEquals(billingCycle, billingTimeService.getBillingCycle(billingCycle, snapshotTime, ZoneId.of("Asia/Tokyo")));
    }

    @ParameterizedTest
    @CsvSource({"America/Los_Angeles,2026-09", "Asia/Tokyo,2026-10"})
    void currentCycleUsesUsersLocalDate(String timezone, String expectedBillingCycle) {
        YearMonth billingCycle = billingTimeService.getBillingCycle(null, snapshotTime, ZoneId.of(timezone));

        assertEquals(YearMonth.parse(expectedBillingCycle), billingCycle);
    }

    @Test
    void monthWindowComesFromCalendarProvider() {
        ZoneId timezone = ZoneId.of("UTC");
        YearMonth billingCycle = YearMonth.of(2026, 10);
        when(billingWindowProvider.find(timezone, billingCycle)).thenReturn(billingWindow);

        assertSame(billingWindow, billingTimeService.getMonthWindow(timezone, billingCycle));
    }

    @ParameterizedTest
    @CsvSource({"2026-10-02T12:00:00Z,true", "2026-10-02T12:15:00Z,true",
            "2026-10-02T12:30:00Z,true", "2026-10-02T12:45:00Z,true",
            "2026-10-02T12:07:00Z,false", "2026-10-02T12:15:01Z,false",
            "2026-10-02T12:15:00.001Z,false", "0000-01-01T00:00:00Z,false",
            "9999-01-01T00:00:00Z,false"})
    void chunkStartMustBeAnExactUtcQuarterHour(String timestamp, boolean expectedValid) {
        assertEquals(expectedValid, billingTimeService.isValidChunkStart(Instant.parse(timestamp)));
    }

    @ParameterizedTest
    @CsvSource({"2026-09-30T23:45:00Z,false", "2026-10-01T00:00:00Z,true",
            "2026-10-31T23:45:00Z,true", "2026-11-01T00:00:00Z,false"})
    void overlapExcludesEventsTouchingOnlyTheOutsideBoundary(String timestamp, boolean expectedOverlap) {
        assertEquals(expectedOverlap, billingTimeService.overlaps(new UsageChunk("event-1", Instant.parse(timestamp)), billingWindow));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-09-30T23:45:00Z", "2026-11-01T00:00:00Z"})
    void outsideChunkDoesNotChangeCounters(String timestamp) {
        UsageAccumulator usageAccumulator = new UsageAccumulator();

        billingTimeService.accumulateUsage(new UsageChunk("outside", Instant.parse(timestamp)), billingWindow, usageAccumulator);

        assertEquals(new UsageSummary(0, 0, 0), usageAccumulator.summary());
    }

    @Test
    void onlyPaidPartOfPeakChunkGetsSurcharge() {
        // Given: 9h55m already used, leaving 5 free minutes out of the 10-hour allowance.
        UsageAccumulator usageAccumulator = new UsageAccumulator();
        usageAccumulator.acceptDuration(35_700);
        UsageChunk usageChunk = new UsageChunk("peak", Instant.parse("2026-10-02T09:00:00Z"));

        // When: count the 15-minute chunk. 09:00-09:05 is free; 09:05-09:15 is paid peak.
        billingTimeService.accumulateUsage(usageChunk, billingWindow, usageAccumulator);

        // Then: 10h10m total, exactly 10h free, and 10 paid peak minutes (600 seconds).
        assertEquals(new UsageSummary(36_600, 36_000, 600), usageAccumulator.summary());
    }

    @Test
    void chunkIsClippedAtBothMonthBoundaries() {
        // A seven-minute offset means a quarter-hour chunk can straddle a local month boundary.
        BillingWindow shortWindow = new BillingWindow(
                Instant.parse("2026-10-01T00:07:00Z"), Instant.parse("2026-10-01T00:12:00Z"));
        UsageAccumulator usageAccumulator = new UsageAccumulator();

        billingTimeService.accumulateUsage(new UsageChunk("clipped", Instant.parse("2026-10-01T00:00:00Z")), shortWindow, usageAccumulator);

        assertEquals(new UsageSummary(300, 300, 0), usageAccumulator.summary());
    }

    @ParameterizedTest
    @CsvSource({"2026-10-02T08:55:00Z,2026-10-02T09:10:00Z,600",
            "2026-10-02T16:55:00Z,2026-10-02T17:10:00Z,300",
            "2026-10-02T17:00:00Z,2026-10-02T17:15:00Z,0",
            "2026-10-02T23:55:00Z,2026-10-03T00:10:00Z,0",
            "2026-10-02T16:00:00Z,2026-10-03T10:00:00Z,7200"})
    void peakSecondsCountOnlyDailyNineToSeventeenUtc(String start, String end, long expectedSeconds) {
        assertEquals(expectedSeconds, billingTimeService.peakSeconds(Instant.parse(start), Instant.parse(end)));
    }
}
