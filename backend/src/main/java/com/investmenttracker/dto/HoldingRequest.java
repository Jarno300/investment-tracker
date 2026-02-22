package com.investmenttracker.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HoldingRequest(
    Long assetId,
    BigDecimal quantity,
    BigDecimal averageCost,
    BigDecimal marketValue,
    Instant updatedAt
) {
}
