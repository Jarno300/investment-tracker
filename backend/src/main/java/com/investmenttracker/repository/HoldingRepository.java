package com.investmenttracker.repository;

import com.investmenttracker.model.Holding;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
  List<Holding> findByUserId(Long userId);
  Optional<Holding> findByUserIdAndAssetId(Long userId, Long assetId);
  Optional<Holding> findByIdAndUserId(Long id, Long userId);
  long countByUserId(Long userId);

  @Query("""
      select distinct upper(trim(a.symbol))
      from Holding h
      join h.asset a
      where h.user.id = :userId
        and a.type = com.investmenttracker.model.AssetType.STOCK
        and a.symbol is not null
        and trim(a.symbol) <> ''
        and h.quantity > 0
      """)
  List<String> findDistinctOwnedStockSymbolsByUserId(@Param("userId") Long userId);
}
