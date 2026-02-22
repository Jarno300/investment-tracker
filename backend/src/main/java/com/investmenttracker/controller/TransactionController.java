package com.investmenttracker.controller;

import com.investmenttracker.dto.TransactionRequest;
import com.investmenttracker.model.Asset;
import com.investmenttracker.model.Transaction;
import com.investmenttracker.model.UserAccount;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.TransactionRepository;
import com.investmenttracker.service.CurrentUserService;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

  private final TransactionRepository transactionRepository;
  private final AssetRepository assetRepository;
  private final CurrentUserService currentUserService;

  public TransactionController(TransactionRepository transactionRepository,
      AssetRepository assetRepository,
      CurrentUserService currentUserService) {
    this.transactionRepository = transactionRepository;
    this.assetRepository = assetRepository;
    this.currentUserService = currentUserService;
  }

  @GetMapping
  public List<Transaction> getAll(@RequestParam(required = false) Long assetId) {
    UserAccount user = currentUserService.getRequiredUser();
    if (assetId != null) {
      return transactionRepository.findByUserIdAndAssetIdOrderByTradedAtDesc(user.getId(), assetId);
    }
    return transactionRepository.findByUserIdOrderByTradedAtDesc(user.getId());
  }

  @GetMapping("/{id}")
  public Transaction getById(@PathVariable Long id) {
    UserAccount user = currentUserService.getRequiredUser();
    return transactionRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Transaction create(@RequestBody TransactionRequest request) {
    UserAccount user = currentUserService.getRequiredUser();
    if (request == null || request.assetId() == null || request.type() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetId and type are required");
    }
    Asset asset = assetRepository.findByIdAndUserId(request.assetId(), user.getId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
    Transaction transaction = new Transaction();
    transaction.setUser(user);
    transaction.setAsset(asset);
    transaction.setType(request.type());
    transaction.setQuantity(request.quantity());
    transaction.setPrice(request.price());
    transaction.setTradedAt(request.tradedAt() == null ? Instant.now() : request.tradedAt());
    transaction.setNotes(request.notes());
    return transactionRepository.save(transaction);
  }
}
