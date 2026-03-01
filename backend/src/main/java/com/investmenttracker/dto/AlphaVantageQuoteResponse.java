package com.investmenttracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlphaVantageQuoteResponse {

  @JsonProperty("Global Quote")
  private GlobalQuote globalQuote;

  @JsonProperty("Note")
  private String note;

  @JsonProperty("Error Message")
  private String errorMessage;

  @JsonProperty("Information")
  private String information;

  public GlobalQuote getGlobalQuote() {
    return globalQuote;
  }

  public void setGlobalQuote(GlobalQuote globalQuote) {
    this.globalQuote = globalQuote;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getInformation() {
    return information;
  }

  public void setInformation(String information) {
    this.information = information;
  }

  public static class GlobalQuote {
    @JsonProperty("05. price")
    private String price;

    @JsonProperty("08. previous close")
    private String previousClose;

    @JsonProperty("09. change")
    private String change;

    @JsonProperty("10. change percent")
    private String changePercent;

    public String getPrice() {
      return price;
    }

    public void setPrice(String price) {
      this.price = price;
    }

    public String getPreviousClose() {
      return previousClose;
    }

    public void setPreviousClose(String previousClose) {
      this.previousClose = previousClose;
    }

    public String getChange() {
      return change;
    }

    public void setChange(String change) {
      this.change = change;
    }

    public String getChangePercent() {
      return changePercent;
    }

    public void setChangePercent(String changePercent) {
      this.changePercent = changePercent;
    }
  }
}
