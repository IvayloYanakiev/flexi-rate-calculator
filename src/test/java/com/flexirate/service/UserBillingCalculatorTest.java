package com.flexirate.service;

import com.flexirate.domain.Bill;
import com.flexirate.domain.BillingWindow;
import com.flexirate.domain.UsageAccumulator;
import com.flexirate.domain.UsageChunk;
import com.flexirate.domain.UsageSummary;
import com.flexirate.domain.User;
import com.flexirate.service.impl.UserBillingCalculatorImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserBillingCalculatorTest {
    private final BillingTimeService billingTimeService = mock(BillingTimeService.class);
    private final PricingPolicy pricingPolicy = mock(PricingPolicy.class);
    private final UserBillingCalculator userBillingCalculator = new UserBillingCalculatorImpl(billingTimeService, pricingPolicy);
    private final User user = new User("alice", false, ZoneId.of("UTC"));
    private final YearMonth billingCycle = YearMonth.of(2026, 10);
    private final BillingWindow billingWindow = new BillingWindow(
            Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"));

    @Test
    void earliestChunksAreProcessedFirstWithEventIdBreakingTies() {
        // Given: the request lists a later event first, and equal-time IDs are out of order.
        UsageChunk laterChunk = new UsageChunk("later", Instant.parse("2026-10-02T10:00:00Z"));
        UsageChunk firstChunk = new UsageChunk("a", Instant.parse("2026-10-02T09:00:00Z"));
        UsageChunk secondChunk = new UsageChunk("b", firstChunk.timestamp());
        List<UsageChunk> usageChunks = new ArrayList<>(List.of(laterChunk, secondChunk, firstChunk));

        userBillingCalculator.calculate(user, billingCycle, billingWindow, usageChunks);

        // Then: the time service receives a, b, later, so grace is allocated chronologically.
        InOrder callOrder = inOrder(billingTimeService);
        callOrder.verify(billingTimeService).accumulateUsage(eq(firstChunk), eq(billingWindow), any(UsageAccumulator.class));
        callOrder.verify(billingTimeService).accumulateUsage(eq(secondChunk), eq(billingWindow), any(UsageAccumulator.class));
        callOrder.verify(billingTimeService).accumulateUsage(eq(laterChunk), eq(billingWindow), any(UsageAccumulator.class));
        assertEquals(List.of(firstChunk, secondChunk, laterChunk), usageChunks);
    }

    @Test
    void completedSummaryIsPassedToPricingAndReturnedBillIsPreserved() {
        // Given: the time collaborator records one complete, fully free 15-minute chunk.
        UsageChunk usageChunk = new UsageChunk("event-1", Instant.parse("2026-10-02T09:00:00Z"));
        doAnswer(invocation -> {
            UsageAccumulator usageAccumulator = invocation.getArgument(2);
            usageAccumulator.acceptDuration(900);
            return null;
        }).when(billingTimeService).accumulateUsage(eq(usageChunk), eq(billingWindow), any(UsageAccumulator.class));
        Bill expectedBill = new Bill("alice", "2026-10", new BigDecimal("0.00"), List.of(), List.of());
        when(pricingPolicy.price(eq(user), eq(billingCycle), any(UsageSummary.class))).thenReturn(expectedBill);

        Bill bill = userBillingCalculator.calculate(user, billingCycle, billingWindow, new ArrayList<>(List.of(usageChunk)));

        // Then: pricing receives seconds, with no early conversion to rounded hours.
        ArgumentCaptor<UsageSummary> usageSummaryCaptor = ArgumentCaptor.forClass(UsageSummary.class);
        verify(pricingPolicy).price(eq(user), eq(billingCycle), usageSummaryCaptor.capture());
        assertEquals(new UsageSummary(900, 900, 0), usageSummaryCaptor.getValue());
        assertSame(expectedBill, bill);
    }

    @Test
    void emptyUsageStillProducesAZeroSummaryForPricing() {
        userBillingCalculator.calculate(user, billingCycle, billingWindow, new ArrayList<>());

        verify(pricingPolicy).price(user, billingCycle, new UsageSummary(0, 0, 0));
        verifyNoInteractions(billingTimeService);
    }
}
