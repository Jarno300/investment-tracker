package com.investmenttracker.controller;

import com.investmenttracker.dto.BuyRequest;
import com.investmenttracker.model.Asset;
import com.investmenttracker.model.AssetType;
import com.investmenttracker.model.Holding;
import com.investmenttracker.model.Transaction;
import com.investmenttracker.model.TransactionType;
import com.investmenttracker.model.UserAccount;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.HoldingRepository;
import com.investmenttracker.repository.TransactionRepository;
import com.investmenttracker.service.CurrentUserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

  private final AssetRepository assetRepository;
  private final HoldingRepository holdingRepository;
  private final TransactionRepository transactionRepository;
  private final CurrentUserService currentUserService;

  public PortfolioController(AssetRepository assetRepository,
      HoldingRepository holdingRepository,
      TransactionRepository transactionRepository,
      CurrentUserService currentUserService) {
    this.assetRepository = assetRepository;
    this.holdingRepository = holdingRepository;
    this.transactionRepository = transactionRepository;
    this.currentUserService = currentUserService;
  }

  @PostMapping("/buy")
  @ResponseStatus(HttpStatus.CREATED)
  public Holding buy(@RequestBody BuyRequest request) {
    UserAccount user = currentUserService.getRequiredUser();
    if (request == null || isBlank(request.symbol()) || isBlank(request.name())
        || request.quantity() == null || request.price() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Symbol, name, quantity and price are required");
    }
    if (request.quantity().compareTo(BigDecimal.ZERO) <= 0 || request.price().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity and price must be greater than zero");
    }

    Asset asset = assetRepository.findByUserIdAndTypeAndSymbolIgnoreCase(
            user.getId(), AssetType.STOCK, request.symbol().trim())
        .orElseGet(() -> {
          Asset created = new Asset();
          created.setUser(user);
          created.setType(AssetType.STOCK);
          created.setSymbol(request.symbol().trim());
          created.setName(request.name().trim());
          created.setCurrency(isBlank(request.currency()) ? "USD" : request.currency().trim());
          return assetRepository.save(created);
        });

    Holding holding = holdingRepository.findByUserIdAndAssetId(user.getId(), asset.getId())
        .orElseGet(() -> {
          Holding created = new Holding();
          created.setUser(user);
          created.setAsset(asset);
          created.setQuantity(BigDecimal.ZERO);
          created.setAverageCost(BigDecimal.ZERO);
          created.setMarketValue(BigDecimal.ZERO);
          created.setUpdatedAt(Instant.now());
          return created;
        });

    BigDecimal oldQty = holding.getQuantity() == null ? BigDecimal.ZERO : holding.getQuantity();
    BigDecimal newQty = oldQty.add(request.quantity());
    BigDecimal oldAvg = holding.getAverageCost() == null ? BigDecimal.ZERO : holding.getAverageCost();
    BigDecimal totalCost = oldAvg.multiply(oldQty).add(request.price().multiply(request.quantity()));
    BigDecimal newAvg = totalCost.divide(newQty, 6, RoundingMode.HALF_UP);

    holding.setQuantity(newQty);
    holding.setAverageCost(newAvg);
    holding.setMarketValue(newQty.multiply(request.price()));
    holding.setUpdatedAt(Instant.now());
    Holding savedHolding = holdingRepository.save(holding);

    Transaction transaction = new Transaction();
    transaction.setUser(user);
    transaction.setAsset(asset);
    transaction.setType(TransactionType.BUY);
    transaction.setQuantity(request.quantity());
    transaction.setPrice(request.price());
    transaction.setTradedAt(request.tradedAt() == null ? Instant.now() : request.tradedAt());
    transactionRepository.save(transaction);

    return savedHolding;
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
