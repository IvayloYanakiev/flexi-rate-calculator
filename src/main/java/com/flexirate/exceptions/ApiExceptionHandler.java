package com.flexirate.exceptions;

import com.flexirate.dto.ErrorDto;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts request-level failures into predictable API errors; recoverable events never reach this advice.
 */
@RestControllerAdvice
public class ApiExceptionHandler {
    /**
     * Preserves the status/code selected for an invalid cycle, user set or oversized batch.
     */
    @ExceptionHandler(BillingException.class)
    public ResponseEntity<ErrorDto> handleBillingException(BillingException billingException) {
        return ResponseEntity
                .status(billingException.status())
                .body(new ErrorDto(billingException.code(), billingException.getMessage()));
    }

    /**
     * Rejects unreadable documents and invalid request DTOs without exposing parser details.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ErrorDto> handleInvalidRequest(Exception exception) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorDto("INVALID_REQUEST", "Provide valid JSON, users with IDs/enterprise flags/timezones, and an events array."));
    }
}
