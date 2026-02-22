package com.investmenttracker.dto;

import com.investmenttracker.model.AssetType;

public record AssetRequest(
    String name,
    AssetType type,
    String symbol,
    String currency
) {
}
