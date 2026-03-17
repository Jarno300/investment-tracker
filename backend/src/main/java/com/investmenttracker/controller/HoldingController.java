package com.investmenttracker.controller;

import com.investmenttracker.dto.HoldingRequest;
import com.investmenttracker.model.Holding;
import com.investmenttracker.service.HoldingService;
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
@RequestMapping("/api/holdings")
public class HoldingController {

  private final HoldingService holdingService;

  public HoldingController(HoldingService holdingService) {
    this.holdingService = holdingService;
  }

  @GetMapping
  public List<Holding> getAll() {
    return holdingService.getAll();
  }

  @GetMapping("/{id}")
  public Holding getById(@PathVariable Long id) {
    return holdingService.getById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Holding create(@RequestBody HoldingRequest request) {
    return holdingService.create(request);
  }

  @PutMapping("/{id}")
  public Holding update(@PathVariable Long id, @RequestBody HoldingRequest request) {
    return holdingService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    holdingService.delete(id);
  }
}
