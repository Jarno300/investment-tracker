package com.investmenttracker.controller;

import com.investmenttracker.dto.StockQuoteResult;
import com.investmenttracker.dto.StockSearchResult;
import com.investmenttracker.service.StockReadService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockSearchController {

  private final StockReadService stockReadService;

  public StockSearchController(StockReadService stockReadService) {
    this.stockReadService = stockReadService;
  }

  @GetMapping("/search")
  public List<StockSearchResult> search(@RequestParam("q") String query) {
    return stockReadService.search(query);
  }

  @GetMapping("/quote")
  public ResponseEntity<StockQuoteResult> quote(@RequestParam("symbol") String symbol) {
    return stockReadService.getQuote(symbol)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
