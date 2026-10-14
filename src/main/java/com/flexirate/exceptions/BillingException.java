package com.flexirate.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Expected billing-request failure with an HTTP status and a stable machine-readable error code.
 */
public class BillingException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    /**
     * Describes a whole-request failure; invalid individual events are counted instead of thrown.
     */
    public BillingException(HttpStatus status, String code, String errorMessage) {
        super(errorMessage);
        this.status = status;
        this.code = code;
    }

    /**
     * Returns the HTTP status used by the exception advice.
     */
    public HttpStatus status() {
        return status;
    }

    /**
     * Returns the stable error identifier exposed in ErrorDto.
     */
    public String code() {
        return code;
    }
}
