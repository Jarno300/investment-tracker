package com.investmenttracker.dto;

import com.investmenttracker.model.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(
    Long assetId,
    TransactionType type,
    BigDecimal quantity,
    BigDecimal price,
    Instant tradedAt,
    String notes
) {
}
