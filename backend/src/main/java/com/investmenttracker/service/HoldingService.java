package com.investmenttracker.service;

import com.investmenttracker.dto.HoldingRequest;
import com.investmenttracker.model.Holding;
import com.investmenttracker.dto.StockQuoteResult;
import com.investmenttracker.model.Asset;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.HoldingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HoldingService {

  private final HoldingRepository holdingRepository;
  private final AssetRepository assetRepository;
  private final CurrentUserService currentUserService;
  private final StockReadService stockReadService;

  public HoldingService(
      HoldingRepository holdingRepository,
      AssetRepository assetRepository,
      CurrentUserService currentUserService,
      StockReadService stockReadService) {
    this.holdingRepository = holdingRepository;
    this.assetRepository = assetRepository;
    this.currentUserService = currentUserService;
    this.stockReadService = stockReadService;
  }

  public List<Holding> getAll() {
    Long userId = currentUserService.getRequiredUser().getId();
    List<Holding> holdings = holdingRepository.findByUserId(userId);
    return enrichMarketValues(holdings);
  }

  public Holding getById(Long id) {
    Long userId = currentUserService.getRequiredUser().getId();
    return holdingRepository
        .findByIdAndUserId(id, userId)
        .map(
            h -> {
              enrichMarketValuesInPlace(List.of(h));
              return h;
            })
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holding not found"));
  }

  public Holding create(HoldingRequest request) {
    if (request == null || request.assetId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetId is required");
    }

    Long userId = currentUserService.getRequiredUser().getId();
    Asset asset =
        assetRepository
            .findByIdAndUserId(request.assetId(), userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));

    Holding holding = new Holding();
    holding.setUser(currentUserService.getRequiredUser());
    holding.setAsset(asset);
    holding.setQuantity(defaultValue(request.quantity()));
    holding.setAverageCost(defaultValue(request.averageCost()));
    holding.setMarketValue(defaultValue(request.marketValue()));
    holding.setUpdatedAt(request.updatedAt() == null ? Instant.now() : request.updatedAt());
    return holdingRepository.save(holding);
  }

  public Holding update(Long id, HoldingRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }

    Long userId = currentUserService.getRequiredUser().getId();
    Holding holding =
        holdingRepository
            .findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holding not found"));

    if (request.assetId() != null) {
      Asset asset =
          assetRepository
              .findByIdAndUserId(request.assetId(), userId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
      holding.setAsset(asset);
    }
    if (request.quantity() != null) {
      holding.setQuantity(request.quantity());
    }
    if (request.averageCost() != null) {
      holding.setAverageCost(request.averageCost());
    }
    if (request.marketValue() != null) {
      holding.setMarketValue(request.marketValue());
    }
    if (request.updatedAt() != null) {
      holding.setUpdatedAt(request.updatedAt());
    }
    return holdingRepository.save(holding);
  }

  public void delete(Long id) {
    Holding holding = getById(id);
    holdingRepository.delete(holding);
  }

  private List<Holding> enrichMarketValues(List<Holding> holdings) {
    if (holdings == null || holdings.isEmpty()) return holdings;
    // Copy to avoid mutating JPA-managed collection unexpectedly for callers.
    List<Holding> enriched = new ArrayList<>(holdings);
    enrichMarketValuesInPlace(enriched);
    return enriched;
  }

  private void enrichMarketValuesInPlace(List<Holding> holdings) {
    for (Holding holding : holdings) {
      if (holding == null || holding.getAsset() == null) continue;
      String symbol = holding.getAsset().getSymbol();
      if (symbol == null || symbol.trim().isEmpty()) continue;

      Optional<StockQuoteResult> quoteOpt = stockReadService.getQuote(symbol);
      if (quoteOpt.isPresent() && quoteOpt.get().price() != null) {
        BigDecimal qty = holding.getQuantity() == null ? BigDecimal.ZERO : holding.getQuantity();
        holding.setMarketValue(quoteOpt.get().price().multiply(qty));
      }
    }
  }

  private static BigDecimal defaultValue(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
