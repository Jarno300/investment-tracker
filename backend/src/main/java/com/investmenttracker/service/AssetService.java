package com.investmenttracker.service;

import com.investmenttracker.dto.AssetRequest;
import com.investmenttracker.model.Asset;
import com.investmenttracker.repository.AssetRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssetService {

  private final AssetRepository assetRepository;
  private final CurrentUserService currentUserService;

  public AssetService(AssetRepository assetRepository, CurrentUserService currentUserService) {
    this.assetRepository = assetRepository;
    this.currentUserService = currentUserService;
  }

  public List<Asset> getAll() {
    Long userId = currentUserService.getRequiredUser().getId();
    return assetRepository.findByUserId(userId);
  }

  public Asset getById(Long id) {
    Long userId = currentUserService.getRequiredUser().getId();
    return assetRepository
        .findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
  }

  public Asset create(AssetRequest request) {
    if (request == null || isBlank(request.name()) || request.type() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset name and type are required");
    }

    Asset asset = new Asset();
    asset.setUser(currentUserService.getRequiredUser());
    asset.setName(request.name().trim());
    asset.setType(request.type());
    asset.setSymbol(normalizeNullable(request.symbol()));
    asset.setCurrency(isBlank(request.currency()) ? "USD" : request.currency().trim().toUpperCase());
    return assetRepository.save(asset);
  }

  public Asset update(Long id, AssetRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
    }

    Asset asset = getById(id);
    if (!isBlank(request.name())) {
      asset.setName(request.name().trim());
    }
    if (request.type() != null) {
      asset.setType(request.type());
    }
    if (request.symbol() != null) {
      asset.setSymbol(normalizeNullable(request.symbol()));
    }
    if (!isBlank(request.currency())) {
      asset.setCurrency(request.currency().trim().toUpperCase());
    }
    return assetRepository.save(asset);
  }

  public void delete(Long id) {
    Asset asset = getById(id);
    assetRepository.delete(asset);
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static String normalizeNullable(String value) {
    return value == null ? null : value.trim();
  }
}
