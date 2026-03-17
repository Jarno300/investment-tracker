package com.investmenttracker.service;

import com.investmenttracker.dto.TransactionRequest;
import com.investmenttracker.model.Asset;
import com.investmenttracker.model.Transaction;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final AssetRepository assetRepository;
  private final CurrentUserService currentUserService;

  public TransactionService(
      TransactionRepository transactionRepository,
      AssetRepository assetRepository,
      CurrentUserService currentUserService) {
    this.transactionRepository = transactionRepository;
    this.assetRepository = assetRepository;
    this.currentUserService = currentUserService;
  }

  public List<Transaction> getAll(Long assetId) {
    Long userId = currentUserService.getRequiredUser().getId();
    if (assetId != null) {
      return transactionRepository.findByUserIdAndAssetIdOrderByTradedAtDesc(userId, assetId);
    }
    return transactionRepository.findByUserIdOrderByTradedAtDesc(userId);
  }

  public Transaction getById(Long id) {
    Long userId = currentUserService.getRequiredUser().getId();
    return transactionRepository
        .findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
  }

  public Transaction create(TransactionRequest request) {
    if (request == null
        || request.assetId() == null
        || request.type() == null
        || request.quantity() == null
        || request.price() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "assetId, type, quantity and price are required");
    }

    Long userId = currentUserService.getRequiredUser().getId();
    Asset asset =
        assetRepository
            .findByIdAndUserId(request.assetId(), userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));

    Transaction transaction = new Transaction();
    transaction.setUser(currentUserService.getRequiredUser());
    transaction.setAsset(asset);
    transaction.setType(request.type());
    transaction.setQuantity(request.quantity());
    transaction.setPrice(request.price());
    transaction.setCosts(defaultValue(request.costs()));
    transaction.setTradedAt(request.tradedAt() == null ? Instant.now() : request.tradedAt());
    transaction.setNotes(request.notes());
    return transactionRepository.save(transaction);
  }

  private static BigDecimal defaultValue(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
