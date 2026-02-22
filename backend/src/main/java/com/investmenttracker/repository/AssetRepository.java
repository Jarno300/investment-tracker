package com.investmenttracker.repository;

import com.investmenttracker.model.Asset;
import com.investmenttracker.model.AssetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {
  List<Asset> findByUserId(Long userId);
  Optional<Asset> findByIdAndUserId(Long id, Long userId);
  Optional<Asset> findByUserIdAndTypeAndSymbolIgnoreCase(Long userId, AssetType type, String symbol);
  long countByUserId(Long userId);
}
