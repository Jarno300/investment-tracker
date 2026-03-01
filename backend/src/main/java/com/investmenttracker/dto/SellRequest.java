package com.investmenttracker.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SellRequest(
    Long holdingId,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal costs,
    Instant tradedAt
) {
}
