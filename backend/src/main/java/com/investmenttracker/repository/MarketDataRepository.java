package com.investmenttracker.repository;

import com.investmenttracker.dto.StockQuoteResult;
import com.investmenttracker.dto.StockSearchResult;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MarketDataRepository {

  private final JdbcTemplate jdbcTemplate;

  public MarketDataRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<StockSearchResult> searchSymbols(String query, int limit) {
    String normalized = query == null ? "" : query.trim().toLowerCase();
    if (normalized.length() < 2) {
      return List.of();
    }

    String like = "%" + normalized + "%";
    String sql = """
        SELECT symbol, company_name
        FROM exchange_symbols
        WHERE LOWER(symbol) LIKE ?
           OR LOWER(company_name) LIKE ?
        ORDER BY
          CASE WHEN LOWER(symbol) = ? THEN 0
               WHEN LOWER(symbol) LIKE ? THEN 1
               ELSE 2
          END,
          symbol
        LIMIT ?
        """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> new StockSearchResult(
            rs.getString("symbol"),
            rs.getString("company_name"),
            "Brussels",
            "EUR"
        ),
        like,
        like,
        normalized,
        normalized + "%",
        limit
    );
  }

  public Optional<StockQuoteResult> findQuoteBySymbol(String symbol) {
    String normalized = normalizeSymbol(symbol);
    if (normalized.isEmpty()) {
      return Optional.empty();
    }

    String sql = """
        SELECT symbol, price, previous_close, change_amount, change_percent
        FROM stock_quote_cache
        WHERE UPPER(symbol) = ?
        LIMIT 1
        """;

    List<StockQuoteResult> quotes = jdbcTemplate.query(
        sql,
        (rs, rowNum) -> new StockQuoteResult(
            rs.getString("symbol"),
            rs.getBigDecimal("price"),
            rs.getBigDecimal("previous_close"),
            rs.getBigDecimal("change_amount"),
            rs.getString("change_percent")
        ),
        normalized
    );

    return quotes.stream().findFirst();
  }

  private static String normalizeSymbol(String symbol) {
    return symbol == null ? "" : symbol.trim().toUpperCase();
  }
}
