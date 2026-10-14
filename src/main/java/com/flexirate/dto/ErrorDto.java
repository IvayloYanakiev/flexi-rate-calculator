package com.flexirate.dto;

/**
 * Stable request-error code and safe user-facing explanation, shared by advice and body-size filtering.
 */
public record ErrorDto(String code, String message) {
}
