package com.investmenttracker.service;

import com.investmenttracker.dto.HoldingRequest;
import com.investmenttracker.model.Asset;
import com.investmenttracker.model.Holding;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.HoldingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HoldingService {

  private final HoldingRepository holdingRepository;
  private final AssetRepository assetRepository;
  private final CurrentUserService currentUserService;

  public HoldingService(
      HoldingRepository holdingRepository,
      AssetRepository assetRepository,
      CurrentUserService currentUserService) {
    this.holdingRepository = holdingRepository;
    this.assetRepository = assetRepository;
    this.currentUserService = currentUserService;
  }

  public List<Holding> getAll() {
    Long userId = currentUserService.getRequiredUser().getId();
    return holdingRepository.findByUserId(userId);
  }

  public Holding getById(Long id) {
    Long userId = currentUserService.getRequiredUser().getId();
    return holdingRepository
        .findByIdAndUserId(id, userId)
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

  private static BigDecimal defaultValue(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
