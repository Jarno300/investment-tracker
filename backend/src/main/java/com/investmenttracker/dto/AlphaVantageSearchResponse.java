package com.investmenttracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AlphaVantageSearchResponse {

  @JsonProperty("bestMatches")
  private List<AlphaVantageMatch> bestMatches;
  @JsonProperty("Note")
  private String note;
  @JsonProperty("Information")
  private String information;
  @JsonProperty("Error Message")
  private String errorMessage;

  public List<AlphaVantageMatch> getBestMatches() {
    return bestMatches;
  }

  public void setBestMatches(List<AlphaVantageMatch> bestMatches) {
    this.bestMatches = bestMatches;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getInformation() {
    return information;
  }

  public void setInformation(String information) {
    this.information = information;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public static class AlphaVantageMatch {
    @JsonProperty("1. symbol")
    private String symbol;
    @JsonProperty("2. name")
    private String name;
    @JsonProperty("4. region")
    private String region;
    @JsonProperty("8. currency")
    private String currency;

    public String getSymbol() {
      return symbol;
    }

    public void setSymbol(String symbol) {
      this.symbol = symbol;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getRegion() {
      return region;
    }

    public void setRegion(String region) {
      this.region = region;
    }

    public String getCurrency() {
      return currency;
    }

    public void setCurrency(String currency) {
      this.currency = currency;
    }
  }
}
