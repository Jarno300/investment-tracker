package com.investmenttracker.dto;

public record StockSearchResult(
    String symbol,
    String name,
    String region,
    String currency
) {
}
