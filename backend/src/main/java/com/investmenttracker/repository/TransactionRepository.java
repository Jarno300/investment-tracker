package com.investmenttracker.repository;

import com.investmenttracker.model.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  List<Transaction> findByAssetIdOrderByTradedAtDesc(Long assetId);
  List<Transaction> findByUserIdOrderByTradedAtDesc(Long userId);
  List<Transaction> findByUserIdAndAssetIdOrderByTradedAtDesc(Long userId, Long assetId);
  Optional<Transaction> findByIdAndUserId(Long id, Long userId);
}
