package com.investmenttracker.controller;

import com.investmenttracker.model.UserAccount;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.HoldingRepository;
import com.investmenttracker.repository.TransactionRepository;
import com.investmenttracker.service.CurrentUserService;
import com.investmenttracker.service.HoldingValuationService;
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
  private final TransactionRepository transactionRepository;
  private final CurrentUserService currentUserService;
  private final HoldingValuationService holdingValuationService;

  public SummaryController(HoldingRepository holdingRepository,
      AssetRepository assetRepository,
      TransactionRepository transactionRepository,
      CurrentUserService currentUserService,
      HoldingValuationService holdingValuationService) {
    this.holdingRepository = holdingRepository;
    this.assetRepository = assetRepository;
    this.transactionRepository = transactionRepository;
    this.currentUserService = currentUserService;
    this.holdingValuationService = holdingValuationService;
  }

  @GetMapping("/summary")
  public Map<String, Object> summary() {
    UserAccount user = currentUserService.getRequiredUser();
    BigDecimal portfolioValue = holdingValuationService.getValuedHoldings(user.getId()).stream()
        .map(holding -> holding.currentValue() == null ? BigDecimal.ZERO : holding.currentValue())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    var userTransactions = transactionRepository.findByUserIdOrderByTradedAtDesc(user.getId());

    BigDecimal totalBuyValue = userTransactions.stream()
        .filter(tx -> tx.getType() == com.investmenttracker.model.TransactionType.BUY)
        .map(tx -> defaultDecimal(tx.getPrice()).multiply(defaultDecimal(tx.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalSellValue = userTransactions.stream()
        .filter(tx -> tx.getType() == com.investmenttracker.model.TransactionType.SELL)
        .map(tx -> defaultDecimal(tx.getPrice()).multiply(defaultDecimal(tx.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalCosts = userTransactions.stream()
        .map(tx -> defaultDecimal(tx.getCosts()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal profitLoss = portfolioValue
        .add(totalSellValue)
        .subtract(totalBuyValue)
        .subtract(totalCosts);

    long holdingsCount = holdingRepository.countByUserId(user.getId());
    long assetsCount = assetRepository.countByUserId(user.getId());
    return Map.of(
        "totalValue", portfolioValue,
        "portfolioValue", portfolioValue,
        "profitLoss", profitLoss,
        "holdingsCount", holdingsCount,
        "assetsCount", assetsCount
    );
  }

  private static BigDecimal defaultDecimal(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
