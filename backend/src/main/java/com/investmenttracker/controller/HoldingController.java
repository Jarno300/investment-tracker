package com.investmenttracker.controller;

import com.investmenttracker.dto.HoldingRequest;
import com.investmenttracker.model.Asset;
import com.investmenttracker.model.Holding;
import com.investmenttracker.model.UserAccount;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.HoldingRepository;
import com.investmenttracker.service.CurrentUserService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/holdings")
public class HoldingController {

  private final HoldingRepository holdingRepository;
  private final AssetRepository assetRepository;
  private final CurrentUserService currentUserService;

  public HoldingController(HoldingRepository holdingRepository,
      AssetRepository assetRepository,
      CurrentUserService currentUserService) {
    this.holdingRepository = holdingRepository;
    this.assetRepository = assetRepository;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public List<Holding> getAll() {
    UserAccount user = currentUserService.getRequiredUser();
    return holdingRepository.findByUserId(user.getId());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Holding create(@RequestBody HoldingRequest request) {
    UserAccount user = currentUserService.getRequiredUser();
    if (request == null || request.assetId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetId is required");
    }
    Asset asset = assetRepository.findByIdAndUserId(request.assetId(), user.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
    Holding holding = new Holding();
    holding.setUser(user);
    holding.setAsset(asset);
    holding.setQuantity(defaultValue(request.quantity()));
    holding.setAverageCost(defaultValue(request.averageCost()));
    holding.setMarketValue(defaultValue(request.marketValue()));
    holding.setUpdatedAt(request.updatedAt() == null ? Instant.now() : request.updatedAt());
    return holdingRepository.save(holding);
  }

  @PutMapping("/{id}")
  public Holding update(@PathVariable Long id, @RequestBody HoldingRequest request) {
    UserAccount user = currentUserService.getRequiredUser();
    Holding holding = holdingRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holding not found"));
    if (request.assetId() != null) {
      Asset asset = assetRepository.findByIdAndUserId(request.assetId(), user.getId())
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

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    UserAccount user = currentUserService.getRequiredUser();
    if (holdingRepository.findByIdAndUserId(id, user.getId()).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Holding not found");
    }
    holdingRepository.deleteById(id);
  }

  private BigDecimal defaultValue(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
