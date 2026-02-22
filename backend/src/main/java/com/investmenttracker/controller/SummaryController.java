package com.investmenttracker.controller;

import com.investmenttracker.model.UserAccount;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.HoldingRepository;
import com.investmenttracker.service.CurrentUserService;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SummaryController {

  private final HoldingRepository holdingRepository;
  private final AssetRepository assetRepository;
  private final CurrentUserService currentUserService;

  public SummaryController(HoldingRepository holdingRepository,
      AssetRepository assetRepository,
      CurrentUserService currentUserService) {
    this.holdingRepository = holdingRepository;
    this.assetRepository = assetRepository;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/summary")
  public Map<String, Object> summary() {
    UserAccount user = currentUserService.getRequiredUser();
    BigDecimal totalValue = holdingRepository.findByUserId(user.getId()).stream()
        .map(holding -> holding.getMarketValue() == null ? BigDecimal.ZERO : holding.getMarketValue())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    long holdingsCount = holdingRepository.countByUserId(user.getId());
    long assetsCount = assetRepository.countByUserId(user.getId());
    return Map.of(
        "totalValue", totalValue,
        "holdingsCount", holdingsCount,
        "assetsCount", assetsCount
    );
  }
}
