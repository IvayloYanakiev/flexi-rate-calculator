package com.flexirate.domain;

import java.math.BigDecimal;

/**
 * Additional charge for paid peak usage only; free peak usage never reaches this component.
 */
public record Surcharge(String type, BigDecimal percentage, BigDecimal billablePeakHours, BigDecimal cost) {
}
