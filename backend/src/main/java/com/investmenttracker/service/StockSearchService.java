package com.investmenttracker.service;

import com.investmenttracker.config.StockApiProperties;
import com.investmenttracker.dto.AlphaVantageSearchResponse;
import com.investmenttracker.dto.StockSearchResult;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class StockSearchService {

  private final RestTemplate restTemplate;
  private final StockApiProperties properties;

  public StockSearchService(StockApiProperties properties) {
    this.restTemplate = new RestTemplate();
    this.properties = properties;
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
}
