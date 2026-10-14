package com.flexirate.service.impl;

import com.flexirate.service.BillingTimeService;
import com.flexirate.domain.UsageAccumulator;

import com.flexirate.domain.UsageChunk;
import com.flexirate.domain.BillingWindow;
import com.flexirate.repository.BillingWindowProvider;

import java.time.*;

import org.springframework.stereotype.Service;

/**
 * Default time policy: local calendar months, UTC peak windows and exact whole-second accounting.
 */
@Service
public class BillingTimeServiceImpl implements BillingTimeService {
    // Supplies the current instant; tests inject a fixed clock so results are repeatable.
    private final Clock billingClock;
    // Supplies local-month boundaries as UTC instants, optionally through the Caffeine cache.
    private final BillingWindowProvider billingWindowProvider;

    /**
     * Injects a controllable clock and a source of local-month UTC boundaries.
     */
    public BillingTimeServiceImpl(Clock billingClock, BillingWindowProvider billingWindowProvider) {
        // Keep the injected clock instead of calling Instant.now() throughout a request.
        this.billingClock = billingClock;
        // Keep calendar lookup separate from usage calculations.
        this.billingWindowProvider = billingWindowProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instant snapshotTime() {
        // Capture one absolute time, e.g. 2026-10-01T00:30Z; the caller reuses it for every user.
        return billingClock.instant();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public YearMonth getBillingCycle(YearMonth explicitBillingCycle, Instant snapshotTime, ZoneId timezone) {
        // An explicit 2026-10 wins. Otherwise convert the captured instant to the user's local month:
        // 2026-10-01T00:30Z is September in Los Angeles, but October in Tokyo.
        return explicitBillingCycle != null ? explicitBillingCycle : YearMonth.from(snapshotTime.atZone(timezone));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BillingWindow getMonthWindow(ZoneId timezone, YearMonth billingCycle) {
        // Europe/Sofia's October 2026 is [Sep 30 21:00Z, Oct 31 22:00Z); DST changes the offset.
        // '[' includes the start; ')' excludes the end, so adjacent months never share a second.
        return billingWindowProvider.find(timezone, billingCycle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidChunkStart(Instant timestamp) {
        // Inspect the UTC clock: an input of 15:15+03:00 represents 12:15Z.
        OffsetDateTime utcStart = timestamp.atOffset(ZoneOffset.UTC);
        return utcStart.getYear() >= 1 // Reject years before the supported range.
                && utcStart.getYear() <= 9998 // Leave room for next-month boundary calculations.
                && utcStart.getMinute() % 15 == 0 // Accept :00/:15/:30/:45; reject :07.
                && utcStart.getSecond() == 0 // Reject 12:15:01Z.
                && utcStart.getNano() == 0; // Reject fractional seconds, e.g. 12:15:00.001Z; billing uses seconds.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean overlaps(UsageChunk usageChunk, BillingWindow billingWindow) {
        // Some usage must start before the month ends AND finish after the month starts.
        // A chunk ending exactly at month start, or starting exactly at month end, is outside.
        return usageChunk.timestamp().isBefore(billingWindow.end()) && usageChunk.end().isAfter(billingWindow.start());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accumulateUsage(UsageChunk usageChunk, BillingWindow billingWindow, UsageAccumulator usageAccumulator) {
        // Keep only the part inside the month. With a +00:07 timezone, October starts Sep 30 23:53Z:
        // a 23:45Z-00:00Z chunk starts counting at 23:53Z, leaving 7 minutes in October.
        Instant clippedStart = later(usageChunk.timestamp(), billingWindow.start());
        // Stop at whichever comes first: the chunk's end or the month's exclusive end.
        Instant clippedEnd = earlier(usageChunk.end(), billingWindow.end());
        // Equal/reversed boundaries mean the chunk contributes no time to this month.
        if (!clippedStart.isBefore(clippedEnd)) {
            // Leave all counters unchanged for an out-of-month chunk.
            return;
        }
        // Convert the retained interval to exact seconds: 15 minutes = 900; 7 minutes = 420.
        long clippedDurationSeconds = Duration.between(clippedStart, clippedEnd).toSeconds();
        // Add usage and consume the remaining free allowance. If 5 free minutes remain in a
        // 09:00Z-09:15Z chunk, this returns 300 free seconds and leaves 600 paid seconds.
        long allocatedFreeSeconds = usageAccumulator.acceptDuration(clippedDurationSeconds);
        // Skip the free prefix: 09:00Z + 300 seconds = 09:05Z, where paid usage begins.
        Instant paidUsageStart = clippedStart.plusSeconds(allocatedFreeSeconds);
        // A fully free chunk has paidUsageStart == clippedEnd, so there is no paid peak usage.
        if (paidUsageStart.isBefore(clippedEnd)) {
            // Count only paid seconds inside UTC peak hours; 09:05Z-09:15Z contributes 600.
            long paidPeakSeconds = peakSeconds(paidUsageStart, clippedEnd);
            // Add those seconds to this user's request-owned counters; pricing applies the surcharge later.
            usageAccumulator.addPaidPeak(paidPeakSeconds);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long peakSeconds(Instant intervalStart, Instant intervalEnd) {
        // Choose calendar dates in UTC because peak hours are independent of the user's timezone.
        LocalDate firstUtcDate = intervalStart.atZone(ZoneOffset.UTC).toLocalDate();
        // Include the ending UTC date when checking windows; an end at midnight adds no overlap there.
        LocalDate lastUtcDate = intervalEnd.atZone(ZoneOffset.UTC).toLocalDate();
        // Start an exact-second total for this paid interval.
        long totalPeakSeconds = 0;
        // Check each touched UTC day. Accepted 15-minute chunks touch at most two UTC dates.
        for (LocalDate utcDate = firstUtcDate; !utcDate.isAfter(lastUtcDate); utcDate = utcDate.plusDays(1)) {
            // Peak starts at 09:00Z: for paid usage 08:55Z-09:10Z, the intersection starts at 09:00Z.
            Instant overlapStart = later(intervalStart, utcDate.atTime(9, 0).toInstant(ZoneOffset.UTC));
            // Peak ends at 17:00Z: for paid usage 16:55Z-17:10Z, the intersection ends at 17:00Z.
            Instant overlapEnd = earlier(intervalEnd, utcDate.atTime(17, 0).toInstant(ZoneOffset.UTC));
            // Count positive intersections only; usage starting at 17:00Z has zero peak overlap.
            if (overlapStart.isBefore(overlapEnd)) {
                // E.g. 16:55Z-17:00Z adds 300 seconds. Do not round hours or money here.
                totalPeakSeconds += Duration.between(overlapStart, overlapEnd).toSeconds();
            }
        }
        // Return the paid peak duration; the pricing policy converts it into a monetary surcharge.
        return totalPeakSeconds;
    }

    /**
     * Chooses the inclusive left boundary of an interval intersection.
     */
    private static Instant later(Instant left, Instant right) {
        // Take the maximum start: later(08:45Z, 09:00Z) = 09:00Z; equal instants are equivalent.
        return left.isAfter(right) ? left : right;
    }

    /**
     * Chooses the exclusive right boundary of an interval intersection.
     */
    private static Instant earlier(Instant left, Instant right) {
        // Take the minimum end: earlier(17:15Z, 17:00Z) = 17:00Z; equal instants are equivalent.
        return left.isBefore(right) ? left : right;
    }
}
