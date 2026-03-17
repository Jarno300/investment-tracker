package com.investmenttracker.service;

import com.investmenttracker.dto.StockQuoteResult;
import com.investmenttracker.dto.StockSearchResult;
import com.investmenttracker.repository.MarketDataRepository;
import com.investmenttracker.repository.RedisMarketDataRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class StockReadService {

  private static final int SEARCH_LIMIT = 25;
  private final RedisMarketDataRepository redisMarketDataRepository;
  private final MarketDataRepository marketDataRepository;

  public StockReadService(
      RedisMarketDataRepository redisMarketDataRepository,
      MarketDataRepository marketDataRepository) {
    this.redisMarketDataRepository = redisMarketDataRepository;
    this.marketDataRepository = marketDataRepository;
  }

  public List<StockSearchResult> search(String query) {
    try {
      List<StockSearchResult> cached = redisMarketDataRepository.searchSymbols(query, SEARCH_LIMIT);
      if (!cached.isEmpty()) {
        return cached;
      }
    } catch (RuntimeException ignored) {
      // Fall back to Postgres when Redis is unavailable.
    }
    return marketDataRepository.searchSymbols(query, SEARCH_LIMIT);
  }

  public Optional<StockQuoteResult> getQuote(String symbol) {
    try {
      Optional<StockQuoteResult> cached = redisMarketDataRepository.findQuoteBySymbol(symbol);
      if (cached.isPresent()) {
        return cached;
      }
    } catch (RuntimeException ignored) {
      // Fall back to Postgres when Redis is unavailable.
    }
    return marketDataRepository.findQuoteBySymbol(symbol);
  }
}
