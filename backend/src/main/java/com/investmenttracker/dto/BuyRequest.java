package com.investmenttracker.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BuyRequest(
    String symbol,
    String name,
    String currency,
    BigDecimal quantity,
    BigDecimal price,
    Instant tradedAt
) {
}
