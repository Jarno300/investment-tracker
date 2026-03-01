package com.investmenttracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_quote_cache")
public class StockQuoteCacheEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String symbol;

  @Column(nullable = false, precision = 19, scale = 6)
  private BigDecimal price;

  @Column(precision = 19, scale = 6)
  private BigDecimal previousClose;

  @Column(precision = 19, scale = 6)
  private BigDecimal changeAmount;

  private String changePercent;

  @Column(nullable = false)
  private Instant lastFetchedAt;

  public Long getId() {
    return id;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public BigDecimal getPreviousClose() {
    return previousClose;
  }

  public void setPreviousClose(BigDecimal previousClose) {
    this.previousClose = previousClose;
  }

  public BigDecimal getChangeAmount() {
    return changeAmount;
  }

  public void setChangeAmount(BigDecimal changeAmount) {
    this.changeAmount = changeAmount;
  }

  public String getChangePercent() {
    return changePercent;
  }

  public void setChangePercent(String changePercent) {
    this.changePercent = changePercent;
  }

  public Instant getLastFetchedAt() {
    return lastFetchedAt;
  }

  public void setLastFetchedAt(Instant lastFetchedAt) {
    this.lastFetchedAt = lastFetchedAt;
  }
}
