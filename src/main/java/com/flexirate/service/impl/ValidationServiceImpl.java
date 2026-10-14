package com.flexirate.service.impl;

import com.flexirate.service.ValidationService;
import com.flexirate.service.BillingTimeService;

import com.flexirate.config.BillingProperties;
import com.flexirate.domain.UsageChunk;
import com.flexirate.domain.User;
import com.flexirate.dto.EventDto;
import com.flexirate.dto.BillingRequestDto;
import com.flexirate.exceptions.BillingException;
import com.flexirate.mapper.BillingMapper;
import com.flexirate.util.BillingDates;
import jakarta.validation.Validator;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Default validation policy: rejects invalid requests but returns null/false for skippable events.
 */
@Service
public class ValidationServiceImpl implements ValidationService {
    private final BillingProperties billingProperties;
    private final Validator beanValidator;
    private final BillingMapper billingMapper;
    private final BillingTimeService billingTimeService;

    /**
     * Wires batch limits, Bean Validation, mapping and UTC-alignment checks.
     */
    public ValidationServiceImpl(BillingProperties billingProperties, Validator beanValidator, BillingMapper billingMapper, BillingTimeService billingTimeService) {
        this.billingProperties = billingProperties;
        this.beanValidator = beanValidator;
        this.billingMapper = billingMapper;
        this.billingTimeService = billingTimeService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public YearMonth validateRequest(BillingRequestDto billingRequestDto) {
        if (billingRequestDto.users().size() > billingProperties.maxUsers() || billingRequestDto.events().size() > billingProperties.maxEvents()) {
            throw new BillingException(HttpStatus.CONTENT_TOO_LARGE, "BATCH_LIMIT", "User or event limit exceeded.");
        }
        String billingCycle = billingRequestDto.billingCycle();
        if (billingCycle == null) {
            return null;
        }
        if (!billingCycle.matches("[0-9]{4}-(0[1-9]|1[0-2])")) {
            throw invalid("billing_cycle must be YYYY-MM.");
        }
        try {
            YearMonth parsedBillingCycle = BillingDates.cycle(billingCycle);
            if (parsedBillingCycle.getYear() < 1 || parsedBillingCycle.getYear() > 9998) {
                throw invalid("Cycle year must be 0001 to 9998.");
            }
            return parsedBillingCycle;
        } catch (DateTimeException dateTimeException) {
            throw invalid("Invalid billing_cycle.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> validateUsers(BillingRequestDto billingRequestDto) {
        List<User> users;
        try {
            users = billingMapper.toUsers(billingRequestDto.users());
        } catch (DateTimeException dateTimeException) {
            throw invalid("Invalid user timezone.");
        }
        Set<String> seenUserIds = HashSet.newHashSet(users.size());
        for (User user : users) {
            if (!seenUserIds.add(user.userId())) {
                throw invalid("Duplicate user_id.");
            }
        }
        return users;
    }

    /**
     * Null means one skipped item; Spring has already performed DTO binding.
     */
    @Override
    public UsageChunk validateChunk(EventDto eventDto) {
        if (eventDto == null || !beanValidator.validate(eventDto).isEmpty()) {
            return null;
        }
        try {
            UsageChunk usageChunk = billingMapper.toChunk(eventDto);
            return billingTimeService.isValidChunkStart(usageChunk.timestamp()) ? usageChunk : null;
        } catch (DateTimeException dateTimeException) {
            return null;
        }
    }

    /**
     * Creates a request-level 400 error; individual event failures instead return null/false.
     */
    private static BillingException invalid(String errorMessage) {
        return new BillingException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", errorMessage);
    }
}
