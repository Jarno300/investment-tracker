package com.investmenttracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.stock")
public class StockApiProperties {

  private String apiKey;
  private String baseUrl = "https://www.alphavantage.co/query";
  private int cacheTtlHours = 24;

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public int getCacheTtlHours() {
    return cacheTtlHours;
  }

  public void setCacheTtlHours(int cacheTtlHours) {
    this.cacheTtlHours = cacheTtlHours;
  }
}
