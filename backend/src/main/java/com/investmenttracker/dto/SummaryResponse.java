package com.investmenttracker.dto;

import java.math.BigDecimal;

public record SummaryResponse(
    BigDecimal totalValue,
    BigDecimal portfolioValue,
    BigDecimal profitLoss,
    long holdingsCount,
    long assetsCount
) {}

