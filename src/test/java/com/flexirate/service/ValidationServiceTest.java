package com.flexirate.service;

import com.flexirate.config.BillingProperties;
import com.flexirate.domain.UsageChunk;
import com.flexirate.domain.User;
import com.flexirate.dto.BillingRequestDto;
import com.flexirate.dto.EventDto;
import com.flexirate.dto.UserDto;
import com.flexirate.exceptions.BillingException;
import com.flexirate.mapper.BillingMapper;
import com.flexirate.service.impl.ValidationServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidationServiceTest {
    private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private final BillingTimeService billingTimeService = mock(BillingTimeService.class);
    private final ValidationService validationService = new ValidationServiceImpl(
            new BillingProperties(1, 1, 1024, false, 1), validatorFactory.getValidator(),
            Mappers.getMapper(BillingMapper.class), billingTimeService);

    @AfterEach
    void closeValidatorFactory() {
        validatorFactory.close();
    }

    /**
     * Parse 2026-10 into YearMonth.of(2026, 10).
     */
    @Test
    void validBillingCycleIsParsed() {
        BillingRequestDto billingRequestDto = new BillingRequestDto(List.of(), List.of(), "2026-10");

        YearMonth billingCycle = validationService.validateRequest(billingRequestDto);

        assertEquals(YearMonth.of(2026, 10), billingCycle);
    }

    /**
     * Return null for an omitted cycle so the time service can resolve each user's current month.
     */
    @Test
    void absentBillingCycleRequestsCurrentMonth() {
        assertNull(validationService.validateRequest(new BillingRequestDto(List.of(), List.of(), null)));
    }

    /**
     * Throw BillingException with HTTP 400 and code INVALID_REQUEST for every malformed or unsupported cycle.
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "2026-1", "2026-00", "2026-13", "0000-01", "9999-12", " 2026-10"})
    void invalidBillingCycleRejectsRequest(String billingCycle) {
        BillingException billingException = assertThrows(BillingException.class,
                () -> validationService.validateRequest(new BillingRequestDto(List.of(), List.of(), billingCycle)));

        assertEquals(HttpStatus.BAD_REQUEST, billingException.status());
        assertEquals("INVALID_REQUEST", billingException.code());
    }

    /**
     * Throw BillingException with HTTP 413 and code BATCH_LIMIT when the configured user limit is exceeded.
     */
    @Test
    void tooManyUsersRejectsRequest() {
        UserDto userDto = new UserDto("alice", false, "UTC");
        BillingRequestDto billingRequestDto = new BillingRequestDto(List.of(userDto, userDto), List.of(), "2026-10");

        BillingException billingException = assertThrows(BillingException.class,
                () -> validationService.validateRequest(billingRequestDto));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, billingException.status());
        assertEquals("BATCH_LIMIT", billingException.code());
    }

    /**
     * Throw BillingException with HTTP 413 when the configured event limit is exceeded.
     */
    @Test
    void tooManyEventsRejectsRequest() {
        EventDto eventDto = validEvent();
        BillingRequestDto billingRequestDto = new BillingRequestDto(List.of(), List.of(eventDto, eventDto), "2026-10");

        BillingException billingException = assertThrows(BillingException.class,
                () -> validationService.validateRequest(billingRequestDto));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, billingException.status());
    }

    /**
     * Preserve alice/bob order, resolve Sofia's ZoneId and preserve bob's enterprise flag.
     */
    @Test
    void validUsersAreMappedInRequestOrder() {
        BillingRequestDto billingRequestDto = new BillingRequestDto(
                List.of(new UserDto("alice", false, "Europe/Sofia"), new UserDto("bob", true, "UTC")),
                List.of(), "2026-10");

        List<User> users = validationService.validateUsers(billingRequestDto);

        assertEquals(List.of("alice", "bob"), users.stream().map(User::userId).toList());
        assertEquals(ZoneId.of("Europe/Sofia"), users.getFirst().timezone());
        assertTrue(users.getLast().enterprise());
    }

    /**
     * Throw BillingException with the exact message "Duplicate user_id.".
     */
    @Test
    void duplicateUsersRejectRequest() {
        UserDto userDto = new UserDto("alice", false, "UTC");

        BillingException billingException = assertThrows(BillingException.class,
                () -> validationService.validateUsers(new BillingRequestDto(List.of(userDto, userDto), List.of(), "2026-10")));

        assertEquals("Duplicate user_id.", billingException.getMessage());
    }

    /**
     * Throw BillingException with the exact message "Invalid user timezone.".
     */
    @Test
    void invalidTimezoneRejectsRequest() {
        UserDto userDto = new UserDto("alice", false, "Invalid/Timezone");

        BillingException billingException = assertThrows(BillingException.class,
                () -> validationService.validateUsers(new BillingRequestDto(List.of(userDto), List.of(), "2026-10")));

        assertEquals("Invalid user timezone.", billingException.getMessage());
    }

    /**
     * Convert 12:00+03:00 to 09:00Z and preserve the event ID in the returned UsageChunk.
     */
    @Test
    void validEventIsNormalizedToUtc() {
        Instant timestamp = Instant.parse("2026-10-02T09:00:00Z");
        when(billingTimeService.isValidChunkStart(timestamp)).thenReturn(true);

        UsageChunk usageChunk = validationService.validateChunk(validEvent());

        assertEquals(new UsageChunk("event-1", timestamp), usageChunk);
    }

    /**
     * When the time-validation collaborator returns false, return null so the caller can count a skipped event.
     */
    @Test
    void offQuarterHourEventIsSkipped() {
        assertNull(validationService.validateChunk(validEvent()));
    }

    /**
     * Return null for missing/non-15 durations before calling the time-validation collaborator.
     */
    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 0, 5, 20, 30})
    void missingOrIncorrectDurationIsSkipped(Integer durationMinutes) {
        EventDto eventDto = new EventDto("event-1", "alice", "2026-10-02T09:00:00Z", durationMinutes);

        assertNull(validationService.validateChunk(eventDto));
        verifyNoInteractions(billingTimeService);
    }

    /**
     * A null event or event with missing fields returns null instead of throwing.
     */
    @Test
    void missingEventOrFieldsAreSkipped() {
        assertNull(validationService.validateChunk(null));
        assertNull(validationService.validateChunk(new EventDto(null, null, null, null)));
    }

    /**
     * An unparseable timestamp returns null and never reaches the time-validation collaborator.
     */
    @Test
    void invalidTimestampIsSkipped() {
        EventDto eventDto = new EventDto("event-1", "alice", "not-a-date", 15);

        assertNull(validationService.validateChunk(eventDto));
        verifyNoInteractions(billingTimeService);
    }

    private EventDto validEvent() {
        return new EventDto("event-1", "alice", "2026-10-02T12:00:00+03:00", 15);
    }
}
