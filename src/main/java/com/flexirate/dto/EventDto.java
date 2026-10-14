package com.flexirate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Raw API event; nullable fields allow the validation service to skip/count incomplete items individually.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventDto(
        @JsonProperty("event_id") @NotBlank @Size(max = 128) String eventId,
        @JsonProperty("user_id") @NotBlank @Size(max = 128) String userId,
        @NotBlank @Size(max = 64) String timestamp,
        @JsonProperty("duration_minutes") @NotNull @Min(15) @Max(15) Integer durationMinutes) {
}
