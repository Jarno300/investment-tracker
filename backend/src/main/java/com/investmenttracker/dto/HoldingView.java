package com.investmenttracker.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record HoldingView(
    Long id,
    AssetView asset,
    BigDecimal quantity,
    BigDecimal marketValue,
    BigDecimal currentValue,
    Instant updatedAt
) {
  public record AssetView(
      Long id,
      String name,
      String symbol,
      String type,
      String currency
  ) {
  }
}
