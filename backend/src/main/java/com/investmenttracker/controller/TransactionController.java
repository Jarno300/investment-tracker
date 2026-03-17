package com.investmenttracker.controller;

import com.investmenttracker.dto.TransactionRequest;
import com.investmenttracker.model.Transaction;
import com.investmenttracker.service.TransactionService;
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

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

  private final TransactionService transactionService;

  public TransactionController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @GetMapping
  public List<Transaction> getAll(@RequestParam(required = false) Long assetId) {
    return transactionService.getAll(assetId);
  }

  @GetMapping("/{id}")
  public Transaction getById(@PathVariable Long id) {
    return transactionService.getById(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Transaction create(@RequestBody TransactionRequest request) {
    return transactionService.create(request);
  }
}
