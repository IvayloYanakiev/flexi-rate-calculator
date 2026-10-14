package com.flexirate.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * API envelope; invalid count is retained for the HTTP header and deliberately excluded from JSON.
 */
public record BillingResponseDto(List<UserBillDto> users, @JsonIgnore int invalidEventCount) {
    /**
     * Snapshots the generated collection so callers cannot mutate the response's user list.
     */
    public BillingResponseDto {
        users = List.copyOf(users);
    }
}
