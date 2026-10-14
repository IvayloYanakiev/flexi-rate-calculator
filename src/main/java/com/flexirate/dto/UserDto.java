package com.flexirate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request identity and billing settings; missing enterprise flags must not silently become false.
 */
public record UserDto(
        @JsonProperty("user_id") @NotBlank @Size(max = 128) String userId,
        @JsonProperty("is_enterprise") @NotNull Boolean enterprise,
        @NotBlank @Size(max = 64) String timezone) {
}
