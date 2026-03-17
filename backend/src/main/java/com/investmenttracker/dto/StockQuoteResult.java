package com.investmenttracker.dto;

import java.math.BigDecimal;

public record StockQuoteResult(
    String symbol,
    BigDecimal price,
    BigDecimal previousClose,
    BigDecimal change,
    String changePercent
) {
}
