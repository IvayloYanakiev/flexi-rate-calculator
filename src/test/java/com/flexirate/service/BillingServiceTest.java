package com.flexirate.service;

import com.flexirate.config.BillingProperties;
import com.flexirate.domain.BillingWindow;
import com.flexirate.domain.UsageAccumulator;
import com.flexirate.domain.UsageChunk;
import com.flexirate.domain.UsageSummary;
import com.flexirate.dto.*;
import com.flexirate.exceptions.BillingException;
import com.flexirate.mapper.BillingMapper;
import com.flexirate.repository.BillingWindowRepository;
import com.flexirate.service.impl.BillingServiceImpl;
import com.flexirate.service.impl.BillingTimeServiceImpl;
import com.flexirate.service.impl.ValidationServiceImpl;
import com.flexirate.service.impl.UserBillingCalculatorImpl;
import com.flexirate.service.impl.NonProgressivePricingPolicy;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class BillingServiceTest {
    private final jakarta.validation.ValidatorFactory validators = jakarta.validation.Validation.buildDefaultValidatorFactory();

    @org.junit.jupiter.api.AfterEach
    void closeValidators() {
        validators.close();
    }

    private final BillingProperties properties = new BillingProperties(1000, 100000, 16777216, true, 1024);
    private final BillingMapper mapper = Mappers.getMapper(BillingMapper.class);
    private final BillingTimeService time = new BillingTimeServiceImpl(
            Clock.fixed(Instant.parse("2026-10-01T00:30:00Z"), ZoneOffset.UTC), new BillingWindowRepository(properties));
    private final BillingService service = new BillingServiceImpl(mapper,
            new ValidationServiceImpl(properties, validators.getValidator(), mapper, time), time,
            new UserBillingCalculatorImpl(time, new NonProgressivePricingPolicy()));

    @ParameterizedTest
    @CsvSource({"false,439,1,99.75", "false,440,2,80.00", "false,2039,2,399.80",
            "false,2040,3,250.00", "true,399,1,89.75", "true,400,2,72.00",
            "true,1999,2,391.80", "true,2000,3,245.00"})
    void wholeMonthRateAndExactThresholds(boolean enterprise, int count, int tier, String cost) {
        // Given: each accepted event adds 15 minutes; the first 40 events consume 10 free hours.
        // When: calculate the complete snapshot and select one rate for every paid hour.
        UserBillDto bill = bill(enterprise, repeatedChunksAtSameTimestamp("u", count, "2026-10-02T00:00:00Z"));
        // Then: assert both tier selection and the total; regular/enterprise thresholds differ.
        assertEquals(tier, bill.tierBreakdown().getFirst().tier());
        assertEquals(new BigDecimal(cost), bill.totalCost());
    }

    @ParameterizedTest
    @CsvSource({"0,0.00", "39,0.00", "40,0.00", "41,0.25"})
    void graceIsTenHours(int count, String expected) {
        assertEquals(new BigDecimal(expected), bill(false, repeatedChunksAtSameTimestamp("u", count, "2026-10-02T00:00:00Z")).totalCost());
    }

    @Test
    void freePeakUsageHasNoSurcharge() {
        UserBillDto bill = bill(false, repeatedChunksAtSameTimestamp("u", 41, "2026-10-02T12:00:00Z"));
        assertEquals(new BigDecimal("0.30"), bill.totalCost());
        assertEquals(new BigDecimal("0.05"), bill.appliedSurcharges().getFirst().cost());
        assertEquals(new BigDecimal("0.25"), bill.appliedSurcharges().getFirst().billablePeakHours());
    }

    @ParameterizedTest
    @CsvSource({"2026-10-02T08:45:00Z,0.00", "2026-10-02T09:00:00Z,0.05",
            "2026-10-02T16:45:00Z,0.05", "2026-10-02T17:00:00Z,0.00",
            "2026-10-02T12:00:00+03:00,0.05"})
    void utcPeakBoundariesAndOffsets(String timestamp, String surcharge) {
        List<EventDto> events = repeatedChunksAtSameTimestamp("u", 40, "2026-10-01T00:00:00Z");
        events.add(event("paid", "u", timestamp));
        UserBillDto bill = bill(false, events);
        BigDecimal actual = bill.appliedSurcharges().isEmpty() ? new BigDecimal("0.00") : bill.appliedSurcharges().getFirst().cost();
        assertEquals(new BigDecimal(surcharge), actual);
        assertEquals(bill.tierBreakdown().getFirst().cost().add(actual), bill.totalCost());
    }

    @Test
    void graceIsAssignedByTimeRatherThanInputOrder() {
        // Given: 40 early chunks consume 10 free hours, but the later peak event is listed first.
        List<EventDto> events = repeatedChunksAtSameTimestamp("u", 40, "2026-10-02T00:00:00Z");
        events.addFirst(event("paid", "u", "2026-10-02T10:00:00Z"));
        // Then: chronological sorting leaves the later 15 minutes paid: $0.25 + $0.05 peak extra.
        assertEquals(new BigDecimal("0.30"), bill(false, events).totalCost());
    }

    @Test
    void monthlyClippingUsesUsersTimezone() {
        BillingRequestDto request = new BillingRequestDto(List.of(new UserDto("u", false, "+00:07")), List.of(
                event("left", "u", "2026-09-30T23:45:00Z"),
                event("right", "u", "2026-10-31T23:45:00Z"),
                event("outside", "u", "2026-11-01T12:00:00Z")), "2026-10");
        UserBillDto bill = service.calculate(request).users().getFirst();
        assertEquals(new BigDecimal("0.25"), bill.tierBreakdown().getFirst().freeHours());
    }

    @Test
    void currentCycleIsPerUserAtOneInstant() {
        BillingRequestDto request = new BillingRequestDto(List.of(new UserDto("west", false, "America/Los_Angeles"),
                new UserDto("east", false, "Asia/Tokyo")), List.of(), null);
        List<UserBillDto> bills = service.calculate(request).users();
        assertEquals("2026-09", bills.get(0).billingCycle());
        assertEquals("2026-10", bills.get(1).billingCycle());
    }

    @Test
    void secondOffsetBoundaryHoursAreRoundedForResponse() {
        BillingResponseDto response = service.calculate(new BillingRequestDto(List.of(new UserDto("u", false, "+00:00:07")),
                List.of(event("crossing", "u", "2026-09-30T23:45:00Z")), "2026-10"));
        assertEquals(0, response.invalidEventCount());
        assertEquals(new BigDecimal("0.00"), response.users().getFirst().tierBreakdown().getFirst().freeHours());
        assertEquals(new BigDecimal("0.00"), response.users().getFirst().totalCost());
    }

    @Test
    void monthlyClippingPreservesSecondsBeforeDisplayRounding() {
        BillingWindow billingWindow = time.getMonthWindow(ZoneId.of("+00:00:07"), YearMonth.of(2026, 10));
        UsageChunk usageChunk = new UsageChunk("crossing", Instant.parse("2026-09-30T23:45:00Z"));
        UsageAccumulator usageAccumulator = new UsageAccumulator();

        time.accumulateUsage(usageChunk, billingWindow, usageAccumulator);

        UsageSummary usageSummary = usageAccumulator.summary();
        assertEquals(7, usageSummary.totalSeconds());
        assertEquals(7, usageSummary.freeSeconds());
        assertEquals(0, usageSummary.paidSeconds());
        assertEquals(0, usageSummary.paidPeakSeconds());
    }

    @Test
    void dstMonthHasCalendarBoundaries() {
        BillingWindowRepository repo = new BillingWindowRepository(properties);
        BillingWindow window = repo.find(ZoneId.of("Europe/Sofia"), YearMonth.of(2026, 10));
        assertEquals(Instant.parse("2026-09-30T21:00:00Z"), window.start());
        assertEquals(Instant.parse("2026-10-31T22:00:00Z"), window.end());
        assertEquals(745, Duration.between(window.start(), window.end()).toHours());
    }

    @Test
    void cacheDisabledProducesTheSameDstWindow() {
        BillingWindowRepository cached = new BillingWindowRepository(properties);
        BillingWindowRepository uncached = new BillingWindowRepository(new BillingProperties(1, 1, 1024, false, 1));
        assertEquals(cached.find(ZoneId.of("Europe/Sofia"), YearMonth.of(2026, 3)),
                uncached.find(ZoneId.of("Europe/Sofia"), YearMonth.of(2026, 3)));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {-15, 0, 5, 10, 14, 16, 25, 30})
    void invalidDurationsAreSkipped(int duration) {
        BillingResponseDto result = service.calculate(new BillingRequestDto(List.of(new UserDto("u", false, "UTC")),
                List.of(new EventDto("a", "u", "2026-10-02T00:00:00Z", duration)), "2026-10"));
        assertEquals(1, result.invalidEventCount());
        assertEquals(new BigDecimal("0.00"), result.users().getFirst().totalCost());
    }

    @Test
    void invalidItemsAndDuplicatesAreSkippedIndependently() {
        List<EventDto> events = Arrays.asList(event("ok", "u", "2026-10-01T00:00:00Z"),
                event("ok", "u", "2026-10-01T00:00:00Z"), event("unknown", "missing", "2026-10-01T00:00:00Z"),
                null, null, new EventDto(null, null, null, null),
                new EventDto("x", "u", null, 15),
                new EventDto("y", "u", "2026-10-01T00:00:00Z", null));
        BillingResponseDto result = service.calculate(new BillingRequestDto(List.of(new UserDto("u", false, "UTC")), events, "2026-10"));
        assertEquals(7, result.invalidEventCount());
        assertEquals(new BigDecimal("0.25"), result.users().getFirst().tierBreakdown().getFirst().freeHours());
    }

    @Test
    void outOfMonthEventsStillReserveTheirIdsBeforeFiltering() {
        BillingResponseDto response = service.calculate(new BillingRequestDto(List.of(new UserDto("u", false, "UTC")),
                List.of(event("same", "u", "2026-09-01T00:00:00Z"),
                        event("same", "u", "2026-10-02T00:00:00Z")), "2026-10"));
        assertEquals(1, response.invalidEventCount());
        assertEquals(new BigDecimal("0.00"), response.users().getFirst().tierBreakdown().getFirst().freeHours());
    }

    @Test
    void invalidEventDoesNotReserveItsId() {
        BillingResponseDto response = service.calculate(new BillingRequestDto(List.of(new UserDto("u", false, "UTC")),
                List.of(event("same", "u", "2026-10-02T12:07:00Z"),
                        event("same", "u", "2026-10-02T12:15:00Z")), "2026-10"));
        assertEquals(1, response.invalidEventCount());
        assertEquals(new BigDecimal("0.25"), response.users().getFirst().tierBreakdown().getFirst().freeHours());
    }

    @Test
    void graceIsPerUserAndDoesNotPersistAcrossSnapshots() {
        List<EventDto> events = repeatedChunksAtSameTimestamp("u", 41, "2026-10-02T00:00:00Z");
        events.addAll(repeatedChunksAtSameTimestamp("v", 41, "2026-10-02T00:00:00Z"));
        BillingRequestDto request = new BillingRequestDto(List.of(new UserDto("u", false, "UTC"), new UserDto("v", false, "UTC")), events, "2026-10");
        BillingResponseDto first = service.calculate(request);
        assertEquals(first, service.calculate(request));
        first.users().forEach(b -> assertEquals(new BigDecimal("0.25"), b.totalCost()));
    }

    @Test
    void invalidCycleTimezoneAndDuplicateUsersRejectRequest() {
        assertThrows(BillingException.class, () -> service.calculate(new BillingRequestDto(List.of(), List.of(), "2026-13")));
        assertThrows(BillingException.class, () -> service.calculate(new BillingRequestDto(List.of(new UserDto("u", false, "Invalid/Zone")), List.of(), null)));
        UserDto user = new UserDto("u", false, "UTC");
        assertThrows(BillingException.class, () -> service.calculate(new BillingRequestDto(List.of(user, user), List.of(), null)));
    }

    @Test
    void concurrentRequestsDoNotShareGraceOrUsage() throws Exception {
        // Each request starts with its own 10 free hours. Additional off-peak chunks cost $0.25 each.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<BigDecimal>> results = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                final int count = 40 + i;
                results.add(executor.submit(() -> bill(false, repeatedChunksAtSameTimestamp("u", count, "2026-10-02T00:00:00Z")).totalCost()));
            }
            for (int i = 0; i < results.size(); i++) {
                assertEquals(BigDecimal.valueOf(i).multiply(new BigDecimal("0.25")).setScale(2), results.get(i).get(10, TimeUnit.SECONDS));
            }
        }
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"2026-10-02T12:00:00Z", "2026-10-02T12:15:00Z",
            "2026-10-02T12:30:00Z", "2026-10-02T12:45:00Z", "2026-10-02T15:15:00+03:00",
            "2026-10-02T12:00:00.000Z"})
    void exactQuarterHourStartsAreAccepted(String timestamp) {
        List<EventDto> events = repeatedChunksAtSameTimestamp("u", 40, "2026-10-01T00:00:00Z");
        events.add(event("paid", "u", timestamp));
        BillingResponseDto response = service.calculate(new BillingRequestDto(List.of(new UserDto("u", false, "UTC")), events, "2026-10"));
        assertEquals(0, response.invalidEventCount());
        assertEquals(new BigDecimal("0.30"), response.users().getFirst().totalCost());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"2026-10-02T12:07:00Z", "2026-10-02T12:14:00Z",
            "2026-10-02T12:15:01Z", "2026-10-02T12:30:00.001Z", "2026-10-02T12:45:00.000000001Z",
            "2026-10-02T12:00:00+00:07"})
    void offQuarterHourEventsAreSkippedAndDoNotConsumeGrace(String timestamp) {
        List<EventDto> events = repeatedChunksAtSameTimestamp("u", 40, "2026-10-01T00:00:00Z");
        events.add(event("invalid", "u", timestamp));
        BillingResponseDto response = service.calculate(new BillingRequestDto(List.of(new UserDto("u", false, "UTC")), events, "2026-10"));
        assertEquals(1, response.invalidEventCount());
        assertEquals(new BigDecimal("0.00"), response.users().getFirst().totalCost());
        assertEquals(new BigDecimal("0.00"), response.users().getFirst().tierBreakdown().getFirst().billableHours());
    }

    private UserBillDto bill(boolean enterprise, List<EventDto> events) {
        return service.calculate(new BillingRequestDto(List.of(new UserDto("u", enterprise, "UTC")), events, "2026-10")).users().getFirst();
    }

    // Distinct IDs at the same instant intentionally exercise additive usage and tier thresholds.
    private List<EventDto> repeatedChunksAtSameTimestamp(String userId, int eventCount, String timestamp) {
        List<EventDto> eventDtos = new ArrayList<>(eventCount);
        for (int eventIndex = 0; eventIndex < eventCount; eventIndex++) {
            eventDtos.add(event(userId + "-" + eventIndex, userId, timestamp));
        }
        return eventDtos;
    }

    private EventDto event(String id, String user, String timestamp) {
        return new EventDto(id, user, timestamp, 15);
    }
}
