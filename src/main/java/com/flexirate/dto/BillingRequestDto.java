package com.flexirate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Events are bound by Spring/Jackson, then validated individually by the service.
 */
public record BillingRequestDto(
        @NotNull List<@NotNull @Valid UserDto> users,
        @NotNull List<EventDto> events,
        @JsonProperty("billing_cycle") String billingCycle) {
}
