package com.investmenttracker.service;

import com.investmenttracker.dto.BuyRequest;
import com.investmenttracker.dto.SellRequest;
import com.investmenttracker.dto.TransactionRequest;
import com.investmenttracker.model.Asset;
import com.investmenttracker.model.AssetType;
import com.investmenttracker.model.Holding;
import com.investmenttracker.model.TransactionType;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.HoldingRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PortfolioService {

  private final AssetRepository assetRepository;
  private final HoldingRepository holdingRepository;
  private final TransactionService transactionService;
  private final CurrentUserService currentUserService;

  public PortfolioService(
      AssetRepository assetRepository,
      HoldingRepository holdingRepository,
      TransactionService transactionService,
      CurrentUserService currentUserService) {
    this.assetRepository = assetRepository;
    this.holdingRepository = holdingRepository;
    this.transactionService = transactionService;
    this.currentUserService = currentUserService;
  }

  @Transactional
  public Holding buy(BuyRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }
    if (isBlank(request.symbol())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is required");
    }
    if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be > 0");
    }
    if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be > 0");
    }
    if (request.costs() != null && request.costs().compareTo(BigDecimal.ZERO) < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "costs cannot be negative");
    }

    var user = currentUserService.getRequiredUser();
    Long userId = user.getId();

    String symbol = request.symbol().trim();
    String normalizedSymbol = symbol.toUpperCase();
    String currency = defaultCurrency(request.currency());

    Asset asset =
        assetRepository
            .findByUserIdAndTypeAndSymbolIgnoreCase(userId, AssetType.STOCK, normalizedSymbol)
            .orElseGet(
                () -> {
                  Asset newAsset = new Asset();
                  newAsset.setUser(user);
                  newAsset.setName(isBlank(request.name()) ? normalizedSymbol : request.name().trim());
                  newAsset.setType(AssetType.STOCK);
                  newAsset.setSymbol(normalizedSymbol);
                  newAsset.setCurrency(currency);
                  return assetRepository.save(newAsset);
                });

    BigDecimal buyQty = request.quantity();
    BigDecimal buyPrice = request.price();
    BigDecimal costs = defaultZero(request.costs());

    Holding holding =
        holdingRepository
            .findByUserIdAndAssetId(userId, asset.getId())
            .orElse(null);

    Instant tradedAt = request.tradedAt() == null ? Instant.now() : request.tradedAt();

    if (holding == null) {
      holding = new Holding();
      holding.setUser(user);
      holding.setAsset(asset);
      holding.setQuantity(buyQty);

      // Average cost per unit includes transaction costs allocated to the new buy.
      BigDecimal totalCostBasis = buyQty.multiply(buyPrice).add(costs);
      BigDecimal avgCost = totalCostBasis.divide(buyQty, 10, RoundingMode.HALF_UP);
      holding.setAverageCost(avgCost);
      holding.setMarketValue(buyQty.multiply(buyPrice));
      holding.setUpdatedAt(tradedAt);
      holding = holdingRepository.save(holding);
    } else {
      BigDecimal oldQty = defaultZero(holding.getQuantity());
      BigDecimal oldAvgCost = defaultZero(holding.getAverageCost());

      BigDecimal newQty = oldQty.add(buyQty);
      if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Invalid quantity state while buying");
      }

      BigDecimal oldTotalCostBasis = oldQty.multiply(oldAvgCost);
      BigDecimal newTotalCostBasis =
          oldTotalCostBasis.add(buyQty.multiply(buyPrice)).add(costs);

      BigDecimal newAvgCost = newTotalCostBasis.divide(newQty, 10, RoundingMode.HALF_UP);
      holding.setQuantity(newQty);
      holding.setAverageCost(newAvgCost);
      holding.setMarketValue(newQty.multiply(buyPrice));
      holding.setUpdatedAt(tradedAt);
      holding = holdingRepository.save(holding);
    }

    TransactionRequest txRequest =
        new TransactionRequest(
            asset.getId(),
            TransactionType.BUY,
            buyQty,
            buyPrice,
            costs,
            tradedAt,
            null);
    transactionService.create(txRequest);

    return holding;
  }

  @Transactional
  public void sell(SellRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }
    if (request.holdingId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "holdingId is required");
    }
    if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be > 0");
    }
    if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be > 0");
    }
    if (request.costs() != null && request.costs().compareTo(BigDecimal.ZERO) < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "costs cannot be negative");
    }

    var user = currentUserService.getRequiredUser();
    Long userId = user.getId();

    Holding holding =
        holdingRepository
            .findByIdAndUserId(request.holdingId(), userId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holding not found"));

    BigDecimal ownedQty = defaultZero(holding.getQuantity());
    BigDecimal sellQty = request.quantity();
    if (sellQty.compareTo(ownedQty) > 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Amount to sell cannot exceed owned quantity");
    }

    BigDecimal sellPrice = request.price();
    BigDecimal costs = defaultZero(request.costs());
    Instant tradedAt = request.tradedAt() == null ? Instant.now() : request.tradedAt();

    TransactionRequest txRequest =
        new TransactionRequest(
            holding.getAsset().getId(),
            TransactionType.SELL,
            sellQty,
            sellPrice,
            costs,
            tradedAt,
            null);
    transactionService.create(txRequest);

    BigDecimal remainingQty = ownedQty.subtract(sellQty);
    if (remainingQty.compareTo(BigDecimal.ZERO) == 0) {
      holdingRepository.delete(holding);
      return;
    }

    holding.setQuantity(remainingQty);
    holding.setMarketValue(remainingQty.multiply(sellPrice));
    holding.setUpdatedAt(tradedAt);
    holdingRepository.save(holding);
  }

  private static BigDecimal defaultZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static String defaultCurrency(String currency) {
    if (isBlank(currency)) return "USD";
    return currency.trim().toUpperCase();
  }
}

