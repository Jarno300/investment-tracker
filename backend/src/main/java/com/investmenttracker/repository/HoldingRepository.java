package com.investmenttracker.repository;

import com.investmenttracker.model.Holding;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
  List<Holding> findByUserId(Long userId);
  Optional<Holding> findByUserIdAndAssetId(Long userId, Long assetId);
  Optional<Holding> findByIdAndUserId(Long id, Long userId);
  long countByUserId(Long userId);
}
