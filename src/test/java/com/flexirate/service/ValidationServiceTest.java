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

    @Test
    void validBillingCycleIsParsed() {
        BillingRequestDto billingRequestDto = new BillingRequestDto(List.of(), List.of(), "2026-10");

        YearMonth billingCycle = validationService.validateRequest(billingRequestDto);

        assertEquals(YearMonth.of(2026, 10), billingCycle);
    }

    @Test
    void absentBillingCycleRequestsCurrentMonth() {
        assertNull(validationService.validateRequest(new BillingRequestDto(List.of(), List.of(), null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "2026-1", "2026-00", "2026-13", "0000-01", "9999-12", " 2026-10"})
    void invalidBillingCycleRejectsRequest(String billingCycle) {
        BillingException billingException = assertThrows(BillingException.class,
                () -> validationService.validateRequest(new BillingRequestDto(List.of(), List.of(), billingCycle)));

        assertEquals(HttpStatus.BAD_REQUEST, billingException.status());
        assertEquals("INVALID_REQUEST", billingException.code());
    }

    @Test
    void tooManyUsersRejectsRequest() {
        UserDto userDto = new UserDto("alice", false, "UTC");
        BillingRequestDto billingRequestDto = new BillingRequestDto(List.of(userDto, userDto), List.of(), "2026-10");

        BillingException billingException = assertThrows(BillingException.class,
                () -> validationService.validateRequest(billingRequestDto));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, billingException.status());
        assertEquals("BATCH_LIMIT", billingException.code());
    }

    @Test
    void tooManyEventsRejectsRequest() {
        EventDto eventDto = validEvent();
        BillingRequestDto billingRequestDto = new BillingRequestDto(List.of(), List.of(eventDto, eventDto), "2026-10");

        BillingException billingException = assertThrows(BillingException.class,
                () -> validationService.validateRequest(billingRequestDto));

        assertEquals(HttpStatus.CONTENT_TOO_LARGE, billingException.status());
    }

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

    @Test
    void duplicateUsersRejectRequest() {
        UserDto userDto = new UserDto("alice", false, "UTC");

        BillingException billingException = assertThrows(BillingException.class,
                () -> validationService.validateUsers(new BillingRequestDto(List.of(userDto, userDto), List.of(), "2026-10")));

        assertEquals("Duplicate user_id.", billingException.getMessage());
    }

    @Test
    void invalidTimezoneRejectsRequest() {
        UserDto userDto = new UserDto("alice", false, "Invalid/Timezone");

        BillingException billingException = assertThrows(BillingException.class,
                () -> validationService.validateUsers(new BillingRequestDto(List.of(userDto), List.of(), "2026-10")));

        assertEquals("Invalid user timezone.", billingException.getMessage());
    }

    @Test
    void validEventIsNormalizedToUtc() {
        Instant timestamp = Instant.parse("2026-10-02T09:00:00Z");
        when(billingTimeService.isValidChunkStart(timestamp)).thenReturn(true);

        UsageChunk usageChunk = validationService.validateChunk(validEvent());

        assertEquals(new UsageChunk("event-1", timestamp), usageChunk);
    }

    @Test
    void offQuarterHourEventIsSkipped() {
        assertNull(validationService.validateChunk(validEvent()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 0, 5, 20, 30})
    void missingOrIncorrectDurationIsSkipped(Integer durationMinutes) {
        EventDto eventDto = new EventDto("event-1", "alice", "2026-10-02T09:00:00Z", durationMinutes);

        assertNull(validationService.validateChunk(eventDto));
        verifyNoInteractions(billingTimeService);
    }

    @Test
    void missingEventOrFieldsAreSkipped() {
        assertNull(validationService.validateChunk(null));
        assertNull(validationService.validateChunk(new EventDto(null, null, null, null)));
    }

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
