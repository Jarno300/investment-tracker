package com.investmenttracker.controller;

import com.investmenttracker.dto.SummaryResponse;
import com.investmenttracker.service.SummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SummaryController {

  private final SummaryService summaryService;

  public SummaryController(SummaryService summaryService) {
    this.summaryService = summaryService;
  }

  @GetMapping("/summary")
  public SummaryResponse getSummary() {
    return summaryService.getSummary();
  }
}

