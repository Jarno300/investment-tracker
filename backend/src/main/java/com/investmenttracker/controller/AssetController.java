package com.investmenttracker.controller;

import com.investmenttracker.dto.AssetRequest;
import com.investmenttracker.model.Asset;
import com.investmenttracker.service.AssetService;
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

@RestController
@RequestMapping("/api/assets")
public class AssetController {

  private final AssetService assetService;

  public AssetController(AssetService assetService) {
    this.assetService = assetService;
  }

  @GetMapping
  public List<Asset> getAll() {
    return assetService.getAll();
  }

  @GetMapping("/{id}")
  public Asset getById(@PathVariable Long id) {
    return assetService.getById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Asset create(@RequestBody AssetRequest request) {
    return assetService.create(request);
  }

  @PutMapping("/{id}")
  public Asset update(@PathVariable Long id, @RequestBody AssetRequest request) {
    return assetService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    assetService.delete(id);
  }
}
