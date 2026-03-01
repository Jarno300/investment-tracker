package com.investmenttracker.service;

import com.investmenttracker.config.StockApiProperties;
import com.investmenttracker.dto.StockQuoteResult;
import com.investmenttracker.model.StockQuoteCacheEntry;
import com.investmenttracker.repository.StockQuoteCacheRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockQuoteCacheService {

  private final StockQuoteCacheRepository repository;
  private final long ttlMs;

  public StockQuoteCacheService(StockApiProperties properties, StockQuoteCacheRepository repository) {
    this.repository = repository;
    this.ttlMs = properties.getCacheTtlHours() * 3600L * 1000L;
  }

  public Optional<StockQuoteResult> get(String symbol) {
    String normalized = normalize(symbol);
    if (normalized.isEmpty()) {
      return Optional.empty();
    }
    Optional<StockQuoteCacheEntry> cached = repository.findBySymbol(normalized);
    if (cached.isEmpty()) {
      return Optional.empty();
    }
    StockQuoteCacheEntry entry = cached.get();
    Instant fetchedAt = entry.getLastFetchedAt();
    if (fetchedAt == null || System.currentTimeMillis() - fetchedAt.toEpochMilli() >= ttlMs) {
      return Optional.empty();
    }
    return Optional.of(new StockQuoteResult(
        entry.getSymbol(),
        entry.getPrice(),
        entry.getPreviousClose(),
        entry.getChangeAmount(),
        entry.getChangePercent()));
  }

  public boolean isStale(String symbol) {
    Optional<StockQuoteResult> cached = get(symbol);
    return cached.isEmpty();
  }

  public boolean wasRefreshedToday(String symbol) {
    String normalized = normalize(symbol);
    if (normalized.isEmpty()) {
      return false;
    }
    LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
    return repository.findBySymbol(normalized)
        .map(StockQuoteCacheEntry::getLastFetchedAt)
        .filter(Objects::nonNull)
        .map(fetchedAt -> fetchedAt.atZone(ZoneOffset.UTC).toLocalDate().isEqual(todayUtc))
        .orElse(false);
  }

  @Transactional
  public void put(String symbol, StockQuoteResult result) {
    String normalized = normalize(symbol);
    if (normalized.isEmpty() || result == null || result.price() == null) {
      return;
    }
    StockQuoteCacheEntry entry = repository.findBySymbol(normalized).orElseGet(StockQuoteCacheEntry::new);
    entry.setSymbol(normalized);
    entry.setPrice(result.price());
    entry.setPreviousClose(result.previousClose());
    entry.setChangeAmount(result.change());
    entry.setChangePercent(result.changePercent());
    entry.setLastFetchedAt(Instant.now());
    repository.save(entry);
  }

  private static String normalize(String symbol) {
    return symbol == null ? "" : symbol.trim().toUpperCase();
  }
}
