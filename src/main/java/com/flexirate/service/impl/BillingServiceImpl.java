package com.flexirate.service.impl;

import com.flexirate.service.BillingService;
import com.flexirate.service.ValidationService;
import com.flexirate.service.BillingTimeService;
import com.flexirate.service.UserBillingCalculator;

import com.flexirate.domain.Bill;
import com.flexirate.domain.BillingBucket;
import com.flexirate.domain.BillingWindow;
import com.flexirate.domain.UsageChunk;
import com.flexirate.domain.User;
import com.flexirate.dto.EventDto;
import com.flexirate.dto.BillingRequestDto;
import com.flexirate.dto.BillingResponseDto;
import com.flexirate.mapper.BillingMapper;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Application facade: prepares one snapshot, delegates billing, and returns the API DTO.
 */
@Service
public class BillingServiceImpl implements BillingService {
    private final BillingMapper billingMapper;
    private final ValidationService validationService;
    private final BillingTimeService billingTimeService;
    private final UserBillingCalculator userBillingCalculator;

    /**
     * Wires the facade to validation, time and calculation contracts.
     */
    public BillingServiceImpl(BillingMapper billingMapper, ValidationService validationService,
                              BillingTimeService billingTimeService, UserBillingCalculator userBillingCalculator) {
        this.billingMapper = billingMapper;
        this.validationService = validationService;
        this.billingTimeService = billingTimeService;
        this.userBillingCalculator = userBillingCalculator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BillingResponseDto calculate(BillingRequestDto billingRequestDto) {
        YearMonth explicitBillingCycle = validationService.validateRequest(billingRequestDto);
        Instant snapshotTime = billingTimeService.snapshotTime();
        Map<String, BillingBucket> billingBuckets = prepareUsers(billingRequestDto, explicitBillingCycle, snapshotTime);
        int invalidEventCount = collectEvents(billingRequestDto.events(), billingBuckets);

        List<Bill> bills = new ArrayList<>(billingBuckets.size());
        for (BillingBucket billingBucket : billingBuckets.values()) {
            Bill bill = userBillingCalculator.calculate(
                    billingBucket.user(),
                    billingBucket.billingCycle(),
                    billingBucket.billingWindow(),
                    billingBucket.usageChunks());
            bills.add(bill);
        }
        // MapStruct generates the entire collection mapping; no DTO mapping loop in this service.
        return billingMapper.toResponse(bills, invalidEventCount);
    }

    /**
     * Creates request-owned contexts in input-user order using one captured clock instant.
     */
    private Map<String, BillingBucket> prepareUsers(BillingRequestDto billingRequestDto, YearMonth explicitBillingCycle, Instant snapshotTime) {
        List<User> users = validationService.validateUsers(billingRequestDto);
        Map<String, BillingBucket> billingBuckets = new LinkedHashMap<>();
        for (User user : users) {
            YearMonth billingCycle = billingTimeService.getBillingCycle(explicitBillingCycle, snapshotTime, user.timezone());
            BillingWindow billingWindow = billingTimeService.getMonthWindow(user.timezone(), billingCycle);
            BillingBucket billingBucket = new BillingBucket(user, billingCycle, billingWindow, new ArrayList<>());
            billingBuckets.put(user.userId(), billingBucket);
        }
        return billingBuckets;
    }

    /**
     * Validates/deduplicates once, groups in-month chunks, and returns the skipped-item count.
     */
    private int collectEvents(List<EventDto> eventDtos, Map<String, BillingBucket> billingBuckets) {
        Set<String> seenEventIds = new HashSet<>();
        int invalidEventCount = 0;
        for (EventDto eventDto : eventDtos) {
            UsageChunk usageChunk = validationService.validateChunk(eventDto);
            if (usageChunk == null) {
                invalidEventCount++;
                continue;
            }

            BillingBucket billingBucket = billingBuckets.get(eventDto.userId());
            // Unknown users are skipped without reserving an ID; add returns false for duplicates.
            if (billingBucket == null || !seenEventIds.add(usageChunk.eventId())) {
                invalidEventCount++;
                continue;
            }
            // Check for overlapping between billing window and event timestamp and add in list
            if (billingTimeService.overlaps(usageChunk, billingBucket.billingWindow())) {
                billingBucket.usageChunks().add(usageChunk);
            }
        }
        return invalidEventCount;
    }

}
