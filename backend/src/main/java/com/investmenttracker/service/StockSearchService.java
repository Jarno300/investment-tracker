package com.investmenttracker.service;

import com.investmenttracker.config.StockApiProperties;
import com.investmenttracker.dto.AlphaVantageSearchResponse;
import com.investmenttracker.dto.AlphaVantageTimeSeriesResponse;
import com.investmenttracker.dto.StockQuoteResult;
import com.investmenttracker.dto.StockSearchResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class StockSearchService {

  private static final Logger log = LoggerFactory.getLogger(StockSearchService.class);
  private final RestTemplate restTemplate;
  private final StockApiProperties properties;
  private final StockQuoteCacheService cache;

  public StockSearchService(StockApiProperties properties, StockQuoteCacheService cache) {
    this.restTemplate = new RestTemplate();
    this.properties = properties;
    this.cache = cache;
  }

  public List<StockSearchResult> search(String query) {
    if (query == null || query.trim().length() < 2) {
      return Collections.emptyList();
    }
    if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stock search API key missing");
    }
    String url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
        .queryParam("function", "SYMBOL_SEARCH")
        .queryParam("keywords", query.trim())
        .queryParam("apikey", properties.getApiKey().trim())
        .toUriString();
    AlphaVantageSearchResponse response =
        restTemplate.getForObject(url, AlphaVantageSearchResponse.class);
    List<AlphaVantageSearchResponse.AlphaVantageMatch> matches =
        Optional.ofNullable(response)
            .map(AlphaVantageSearchResponse::getBestMatches)
            .orElse(Collections.emptyList());
    return matches.stream()
        .map(match -> new StockSearchResult(
            match.getSymbol(),
            match.getName(),
            match.getRegion(),
            match.getCurrency()))
        .collect(Collectors.toList());
  }

  public StockQuoteResult getQuote(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      log.debug("getQuote called with null/blank symbol");
      return null;
    }
    Optional<StockQuoteResult> cached = cache.get(symbol);
    if (cached.isPresent()) {
      StockQuoteResult result = cached.get();
      if (result.price() != null) {
        log.debug("Quote cache hit for [{}]", symbol);
        return result;
      }
      log.debug("Cached quote for [{}] has no price, fetching fresh", symbol);
    }
    log.info("Fetching quote from API for [{}]", symbol);
    return fetchQuoteFromApi(symbol);
  }

  public StockQuoteResult refreshQuote(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      log.debug("refreshQuote called with null/blank symbol");
      return null;
    }
    log.info("Refreshing quote from API for [{}]", symbol);
    return fetchQuoteFromApi(symbol);
  }

  private StockQuoteResult fetchQuoteFromApi(String symbol) {
    if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stock API key missing");
    }
    String url = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
        .queryParam("function", "TIME_SERIES_DAILY")
        .queryParam("symbol", symbol.trim())
        .queryParam("apikey", properties.getApiKey().trim())
        .toUriString();
    AlphaVantageTimeSeriesResponse response =
        restTemplate.getForObject(url, AlphaVantageTimeSeriesResponse.class);
    if (response != null) {
      String errorMsg = response.getNote();
      if (errorMsg == null || errorMsg.isBlank()) {
        errorMsg = response.getErrorMessage();
      }
      if (errorMsg == null || errorMsg.isBlank()) {
        errorMsg = response.getInformation();
      }
      if (errorMsg != null && !errorMsg.isBlank()) {
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Stock quote service: " + errorMsg.replaceAll("<[^>]+>", "").trim());
      }
    }
    Map<String, AlphaVantageTimeSeriesResponse.DailyData> series =
        Optional.ofNullable(response)
            .map(AlphaVantageTimeSeriesResponse::getTimeSeriesDaily)
            .orElse(null);
    if (series == null || series.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "No quote data for this symbol. Check your API key and try again.");
    }
    String latestDate = series.keySet().stream().sorted().reduce((a, b) -> b).orElse(null);
    if (latestDate == null) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "No quote data for this symbol.");
    }
    AlphaVantageTimeSeriesResponse.DailyData latest = series.get(latestDate);
    if (latest == null || latest.getClose() == null || latest.getClose().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "No quote data for this symbol.");
    }
    BigDecimal price = parseDecimal(latest.getClose());
    if (price == null) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Invalid quote data for this symbol.");
    }
    String previousDate = series.keySet().stream()
        .sorted()
        .filter(d -> d.compareTo(latestDate) < 0)
        .reduce((a, b) -> b)
        .orElse(null);
    BigDecimal previousClose = null;
    BigDecimal change = null;
    String changePercent = null;
    if (previousDate != null) {
      AlphaVantageTimeSeriesResponse.DailyData prev = series.get(previousDate);
      if (prev != null && prev.getClose() != null) {
        previousClose = parseDecimal(prev.getClose());
        if (previousClose != null && previousClose.compareTo(BigDecimal.ZERO) != 0) {
          change = price.subtract(previousClose);
          changePercent = String.format("%.2f%%",
              change.multiply(BigDecimal.valueOf(100)).divide(previousClose, 4, RoundingMode.HALF_UP));
        }
      }
    }
    StockQuoteResult result = new StockQuoteResult(
        symbol.trim(),
        price,
        previousClose,
        change,
        changePercent);
    cache.put(symbol.trim(), result);
    return result;
  }

  private static BigDecimal parseDecimal(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
