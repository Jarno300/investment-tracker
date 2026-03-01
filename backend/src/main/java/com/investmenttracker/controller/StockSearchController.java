package com.investmenttracker.controller;

import com.investmenttracker.dto.StockQuoteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.investmenttracker.dto.StockSearchResult;
import com.investmenttracker.service.StockSearchService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockSearchController {

  private static final Logger log = LoggerFactory.getLogger(StockSearchController.class);
  private final StockSearchService stockSearchService;

  public StockSearchController(StockSearchService stockSearchService) {
    this.stockSearchService = stockSearchService;
  }

  @GetMapping("/search")
  public List<StockSearchResult> search(@RequestParam("q") String query) {
    return stockSearchService.search(query);
  }

  @GetMapping("/quote")
  public ResponseEntity<StockQuoteResult> quote(@RequestParam("symbol") String symbol) {
    log.info("Quote requested for symbol: [{}]", symbol);
    StockQuoteResult quote = stockSearchService.getQuote(symbol);
    if (quote == null) {
      log.warn("Quote not found for symbol: [{}]", symbol);
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(quote);
  }
}
