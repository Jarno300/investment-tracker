package com.investmenttracker.service;

import com.investmenttracker.dto.SummaryResponse;
import com.investmenttracker.dto.StockQuoteResult;
import com.investmenttracker.model.Holding;
import com.investmenttracker.model.UserAccount;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.HoldingRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SummaryService {

  private final HoldingRepository holdingRepository;
  private final AssetRepository assetRepository;
  private final StockReadService stockReadService;
  private final CurrentUserService currentUserService;

  public SummaryService(
      HoldingRepository holdingRepository,
      AssetRepository assetRepository,
      StockReadService stockReadService,
      CurrentUserService currentUserService) {
    this.holdingRepository = holdingRepository;
    this.assetRepository = assetRepository;
    this.stockReadService = stockReadService;
    this.currentUserService = currentUserService;
  }

  public SummaryResponse getSummary() {
    UserAccount user = currentUserService.getRequiredUser();
    Long userId = user.getId();

    List<Holding> holdings = holdingRepository.findByUserId(userId);

    BigDecimal totalValue = BigDecimal.ZERO;
    BigDecimal profitLoss = BigDecimal.ZERO;

    for (Holding holding : holdings) {
      if (holding == null || holding.getQuantity() == null || holding.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
        continue;
      }
      if (holding.getAsset() == null) continue;

      String symbol = holding.getAsset().getSymbol();
      if (symbol == null || symbol.trim().isEmpty()) continue;

      Optional<StockQuoteResult> quoteOpt = stockReadService.getQuote(symbol);
      if (quoteOpt.isEmpty() || quoteOpt.get().price() == null) continue;

      BigDecimal qty = holding.getQuantity();
      BigDecimal currentPrice = quoteOpt.get().price();
      BigDecimal currentValue = currentPrice.multiply(qty);

      BigDecimal totalInvestment = holding.getAverageCost().multiply(qty);
      totalValue = totalValue.add(currentValue);
      profitLoss = profitLoss.add(currentValue.subtract(totalInvestment));
    }

    long holdingsCount = holdingRepository.countByUserId(userId);
    long assetsCount = assetRepository.countByUserId(userId);

    return new SummaryResponse(
        totalValue,
        totalValue,
        profitLoss,
        holdingsCount,
        assetsCount);
  }
}

