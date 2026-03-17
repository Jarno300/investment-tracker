package com.investmenttracker.service;

import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.HoldingRepository;
import com.investmenttracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final CurrentUserService currentUserService;
  private final TransactionRepository transactionRepository;
  private final HoldingRepository holdingRepository;
  private final AssetRepository assetRepository;

  public UserService(
      CurrentUserService currentUserService,
      TransactionRepository transactionRepository,
      HoldingRepository holdingRepository,
      AssetRepository assetRepository) {
    this.currentUserService = currentUserService;
    this.transactionRepository = transactionRepository;
    this.holdingRepository = holdingRepository;
    this.assetRepository = assetRepository;
  }

  @Transactional
  public void refreshCurrentUserAccountData() {
    Long userId = currentUserService.getRequiredUser().getId();
    transactionRepository.deleteByUserId(userId);
    holdingRepository.deleteByUserId(userId);
    assetRepository.deleteByUserId(userId);
  }
}
