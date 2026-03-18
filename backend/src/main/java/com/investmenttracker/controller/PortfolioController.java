package com.investmenttracker.controller;

import com.investmenttracker.dto.BuyRequest;
import com.investmenttracker.dto.SellRequest;
import com.investmenttracker.model.Holding;
import com.investmenttracker.service.PortfolioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

  private final PortfolioService portfolioService;

  public PortfolioController(PortfolioService portfolioService) {
    this.portfolioService = portfolioService;
  }

  @PostMapping("/buy")
  public Holding buy(@RequestBody BuyRequest request) {
    return portfolioService.buy(request);
  }

  @PostMapping("/sell")
  public ResponseEntity<Void> sell(@RequestBody SellRequest request) {
    portfolioService.sell(request);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}

