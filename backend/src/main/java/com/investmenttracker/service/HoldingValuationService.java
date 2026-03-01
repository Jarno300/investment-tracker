package com.investmenttracker.service;

import com.investmenttracker.dto.HoldingView;
import com.investmenttracker.model.AssetType;
import com.investmenttracker.model.Holding;
import com.investmenttracker.model.StockQuoteCacheEntry;
import com.investmenttracker.repository.HoldingRepository;
import com.investmenttracker.repository.StockQuoteCacheRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class HoldingValuationService {

  private final HoldingRepository holdingRepository;
  private final StockQuoteCacheRepository stockQuoteCacheRepository;
  private final StockSearchService stockSearchService;

  public HoldingValuationService(HoldingRepository holdingRepository,
      StockQuoteCacheRepository stockQuoteCacheRepository,
      StockSearchService stockSearchService) {
    this.holdingRepository = holdingRepository;
    this.stockQuoteCacheRepository = stockQuoteCacheRepository;
    this.stockSearchService = stockSearchService;
  }

  public List<HoldingView> getValuedHoldings(Long userId) {
    List<Holding> holdings = holdingRepository.findByUserId(userId);
    Set<String> stockSymbols = holdings.stream()
        .filter(h -> h.getAsset() != null && h.getAsset().getType() == AssetType.STOCK)
        .map(h -> normalize(h.getAsset().getSymbol()))
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());

    Map<String, StockQuoteCacheEntry> quoteBySymbol = stockQuoteCacheRepository.findBySymbolIn(stockSymbols).stream()
        .collect(Collectors.toMap(
            entry -> normalize(entry.getSymbol()),
            Function.identity(),
            (first, second) -> first));

    List<String> missingSymbols = new ArrayList<>();
    for (String symbol : stockSymbols) {
      StockQuoteCacheEntry cached = quoteBySymbol.get(symbol);
      if (cached == null || cached.getPrice() == null) {
        missingSymbols.add(symbol);
      }
    }
    for (String symbol : missingSymbols) {
      try {
        stockSearchService.getQuote(symbol);
      } catch (Exception ignored) {
        // Keep response available even when quote refresh fails.
      }
    }
    if (!missingSymbols.isEmpty()) {
      quoteBySymbol = stockQuoteCacheRepository.findBySymbolIn(stockSymbols).stream()
          .collect(Collectors.toMap(
              entry -> normalize(entry.getSymbol()),
              Function.identity(),
              (first, second) -> first));
    }
    final Map<String, StockQuoteCacheEntry> finalQuoteBySymbol = quoteBySymbol;

    return holdings.stream()
        .map(holding -> toView(holding, finalQuoteBySymbol))
        .toList();
  }

  private HoldingView toView(Holding holding, Map<String, StockQuoteCacheEntry> quoteBySymbol) {
    BigDecimal quantity = defaultDecimal(holding.getQuantity());
    BigDecimal investmentValue = defaultDecimal(holding.getMarketValue());
    BigDecimal currentValue = investmentValue;

    if (holding.getAsset() != null && holding.getAsset().getType() == AssetType.STOCK) {
      String symbol = normalize(holding.getAsset().getSymbol());
      StockQuoteCacheEntry quote = quoteBySymbol.get(symbol);
      if (quote != null && quote.getPrice() != null) {
        currentValue = quote.getPrice().multiply(quantity);
      }
    }

    HoldingView.AssetView assetView = null;
    if (holding.getAsset() != null) {
      assetView = new HoldingView.AssetView(
          holding.getAsset().getId(),
          holding.getAsset().getName(),
          holding.getAsset().getSymbol(),
          holding.getAsset().getType() == null ? null : holding.getAsset().getType().name(),
          holding.getAsset().getCurrency());
    }

    return new HoldingView(
        holding.getId(),
        assetView,
        quantity,
        investmentValue,
        currentValue,
        holding.getUpdatedAt());
  }

  private static BigDecimal defaultDecimal(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static String normalize(String symbol) {
    return symbol == null ? "" : symbol.trim().toUpperCase();
  }
}
