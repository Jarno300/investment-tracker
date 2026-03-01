package com.investmenttracker.repository;

import com.investmenttracker.model.StockQuoteCacheEntry;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockQuoteCacheRepository extends JpaRepository<StockQuoteCacheEntry, Long> {
  Optional<StockQuoteCacheEntry> findBySymbol(String symbol);
  List<StockQuoteCacheEntry> findBySymbolIn(Collection<String> symbols);
}
