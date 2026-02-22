package com.investmenttracker.controller;

import com.investmenttracker.dto.StockSearchResult;
import com.investmenttracker.service.StockSearchService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockSearchController {

  private final StockSearchService stockSearchService;

  public StockSearchController(StockSearchService stockSearchService) {
    this.stockSearchService = stockSearchService;
  }

  @GetMapping("/search")
  public List<StockSearchResult> search(@RequestParam("q") String query) {
    return stockSearchService.search(query);
  }
}
