package com.flexirate.domain;

import java.time.ZoneId;

/**
 * Normalized user settings used by billing; this is a value object, not a persistence entity.
 */
public record User(String userId, boolean enterprise, ZoneId timezone) {
}
