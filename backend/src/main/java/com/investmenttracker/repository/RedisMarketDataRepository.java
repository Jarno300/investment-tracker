package com.investmenttracker.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investmenttracker.dto.StockQuoteResult;
import com.investmenttracker.dto.StockSearchResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisMarketDataRepository {

  private static final String REDIS_QUOTES_KEY = "br:stocks:quotes";
  private static final String REDIS_METADATA_KEY = "br:stocks:metadata";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final StringRedisTemplate redisTemplate;

  public RedisMarketDataRepository(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public List<StockSearchResult> searchSymbols(String query, int limit) {
    String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    if (normalized.length() < 2) {
      return List.of();
    }

    Map<Object, Object> entries = redisTemplate.opsForHash().entries(REDIS_METADATA_KEY);
    if (entries.isEmpty()) {
      return List.of();
    }

    List<StockSearchResult> results = new ArrayList<>();
    for (Map.Entry<Object, Object> entry : entries.entrySet()) {
      if (!(entry.getValue() instanceof String json)) {
        continue;
      }
      try {
        Map<String, Object> parsed = parseJsonMap(json);
        String symbol = stringValue(parsed.get("symbol"));
        String name = stringValue(parsed.get("name"));
        if (symbol == null || name == null) {
          continue;
        }
        String matchSpace = (symbol + " " + name).toLowerCase(Locale.ROOT);
        if (!matchSpace.contains(normalized)) {
          continue;
        }
        results.add(
            new StockSearchResult(
                symbol,
                name,
                stringValue(parsed.getOrDefault("region", "Brussels")),
                stringValue(parsed.getOrDefault("currency", "EUR"))));
        if (results.size() >= limit) {
          break;
        }
      } catch (IOException ignored) {
        // Skip malformed cache entries and continue.
      }
    }

    return results;
  }

  public Optional<StockQuoteResult> findQuoteBySymbol(String symbol) {
    String normalized = normalizeSymbol(symbol);
    if (normalized.isEmpty()) {
      return Optional.empty();
    }

    Object raw = redisTemplate.opsForHash().get(REDIS_QUOTES_KEY, normalized);
    if (!(raw instanceof String json)) {
      return Optional.empty();
    }

    try {
      Map<String, Object> parsed = parseJsonMap(json);
      return Optional.of(
          new StockQuoteResult(
              stringValue(parsed.getOrDefault("symbol", normalized)),
              decimalValue(parsed.get("price")),
              decimalValue(parsed.get("previousClose")),
              decimalValue(parsed.get("change")),
              stringValue(parsed.get("changePercent"))));
    } catch (IOException ignored) {
      return Optional.empty();
    }
  }

  private Map<String, Object> parseJsonMap(String json) throws IOException {
    return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
  }

  private static BigDecimal decimalValue(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return new BigDecimal(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private static String normalizeSymbol(String symbol) {
    return symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
  }
}
