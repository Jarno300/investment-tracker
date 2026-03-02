package com.investmenttracker.controller;

import com.investmenttracker.model.UserAccount;
import com.investmenttracker.repository.AssetRepository;
import com.investmenttracker.repository.HoldingRepository;
import com.investmenttracker.repository.TransactionRepository;
import com.investmenttracker.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

  private final TransactionRepository transactionRepository;
  private final HoldingRepository holdingRepository;
  private final AssetRepository assetRepository;
  private final CurrentUserService currentUserService;

  public AccountController(TransactionRepository transactionRepository,
      HoldingRepository holdingRepository,
      AssetRepository assetRepository,
      CurrentUserService currentUserService) {
    this.transactionRepository = transactionRepository;
    this.holdingRepository = holdingRepository;
    this.assetRepository = assetRepository;
    this.currentUserService = currentUserService;
  }

  @PostMapping("/refresh")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Transactional
  public void refreshAccountData() {
    UserAccount user = currentUserService.getRequiredUser();
    Long userId = user.getId();
    transactionRepository.deleteByUserId(userId);
    holdingRepository.deleteByUserId(userId);
    assetRepository.deleteByUserId(userId);
  }
}
