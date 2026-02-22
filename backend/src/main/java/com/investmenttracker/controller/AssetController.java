package com.investmenttracker.controller;

import com.investmenttracker.dto.AssetRequest;
import com.investmenttracker.model.Asset;
import com.investmenttracker.model.UserAccount;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.service.CurrentUserService;
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
@RequestMapping("/api/assets")
public class AssetController {

  private final AssetRepository assetRepository;
  private final CurrentUserService currentUserService;

  public AssetController(AssetRepository assetRepository, CurrentUserService currentUserService) {
    this.assetRepository = assetRepository;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public List<Asset> getAll() {
    UserAccount user = currentUserService.getRequiredUser();
    return assetRepository.findByUserId(user.getId());
  }

  @GetMapping("/{id}")
  public Asset getById(@PathVariable Long id) {
    UserAccount user = currentUserService.getRequiredUser();
    return assetRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Asset create(@RequestBody AssetRequest request) {
    UserAccount user = currentUserService.getRequiredUser();
    if (request == null || request.name() == null || request.name().isBlank() || request.type() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset name and type are required");
    }
    Asset asset = new Asset();
    asset.setUser(user);
    asset.setName(request.name().trim());
    asset.setType(request.type());
    asset.setSymbol(request.symbol());
    asset.setCurrency(request.currency() == null ? "USD" : request.currency().trim());
    return assetRepository.save(asset);
  }

  @PutMapping("/{id}")
  public Asset update(@PathVariable Long id, @RequestBody AssetRequest request) {
    UserAccount user = currentUserService.getRequiredUser();
    Asset asset = assetRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
    if (request.name() != null && !request.name().isBlank()) {
      asset.setName(request.name().trim());
    }
    if (request.type() != null) {
      asset.setType(request.type());
    }
    if (request.symbol() != null) {
      asset.setSymbol(request.symbol().trim());
    }
    if (request.currency() != null && !request.currency().isBlank()) {
      asset.setCurrency(request.currency().trim());
    }
    return assetRepository.save(asset);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    UserAccount user = currentUserService.getRequiredUser();
    if (assetRepository.findByIdAndUserId(id, user.getId()).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found");
    }
    assetRepository.deleteById(id);
  }
}
