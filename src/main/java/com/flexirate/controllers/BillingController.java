package com.flexirate.controllers;

import com.flexirate.dto.BillingRequestDto;
import com.flexirate.dto.BillingResponseDto;
import com.flexirate.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP adapter: delegates through the service interface and translates response metadata into a header.
 */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final BillingService billingService;

    /**
     * Injects the application contract rather than a particular service implementation.
     */
    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * Returns mapped bills and the skipped-event count; request-level validation errors are handled by advice.
     */
    @PostMapping(value = "/calculate", consumes = "application/json", produces = "application/json")
    public ResponseEntity<BillingResponseDto> calculate(@Valid @RequestBody BillingRequestDto billingRequestDto) {
        BillingResponseDto billingResponseDto = billingService.calculate(billingRequestDto);
        return ResponseEntity.ok()
                .header("X-Invalid-Event-Count", Integer.toString(billingResponseDto.invalidEventCount()))
                .body(billingResponseDto);
    }
}
