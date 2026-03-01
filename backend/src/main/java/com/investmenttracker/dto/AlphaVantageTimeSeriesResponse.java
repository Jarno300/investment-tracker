package com.investmenttracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class AlphaVantageTimeSeriesResponse {

  @JsonProperty("Meta Data")
  private MetaData metaData;

  @JsonProperty("Time Series (Daily)")
  private Map<String, DailyData> timeSeriesDaily;

  @JsonProperty("Note")
  private String note;

  @JsonProperty("Error Message")
  private String errorMessage;

  @JsonProperty("Information")
  private String information;

  public MetaData getMetaData() {
    return metaData;
  }

  public void setMetaData(MetaData metaData) {
    this.metaData = metaData;
  }

  public Map<String, DailyData> getTimeSeriesDaily() {
    return timeSeriesDaily;
  }

  public void setTimeSeriesDaily(Map<String, DailyData> timeSeriesDaily) {
    this.timeSeriesDaily = timeSeriesDaily;
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

  public static class MetaData {
    @JsonProperty("1. Information")
    private String information;

    @JsonProperty("2. Symbol")
    private String symbol;

    @JsonProperty("3. Last Refreshed")
    private String lastRefreshed;

    @JsonProperty("4. Output Size")
    private String outputSize;

    @JsonProperty("5. Time Zone")
    private String timeZone;

    public String getInformation() { return information; }
    public void setInformation(String information) { this.information = information; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getLastRefreshed() { return lastRefreshed; }
    public void setLastRefreshed(String lastRefreshed) { this.lastRefreshed = lastRefreshed; }
    public String getOutputSize() { return outputSize; }
    public void setOutputSize(String outputSize) { this.outputSize = outputSize; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
  }

  /** Value type for each date entry in Time Series (Daily) */
  public static class DailyData {
    @JsonProperty("1. open")
    private String open;

    @JsonProperty("2. high")
    private String high;

    @JsonProperty("3. low")
    private String low;

    @JsonProperty("4. close")
    private String close;

    @JsonProperty("5. volume")
    private String volume;

    public String getOpen() { return open; }
    public void setOpen(String open) { this.open = open; }
    public String getHigh() { return high; }
    public void setHigh(String high) { this.high = high; }
    public String getLow() { return low; }
    public void setLow(String low) { this.low = low; }
    public String getClose() { return close; }
    public void setClose(String close) { this.close = close; }
    public String getVolume() { return volume; }
    public void setVolume(String volume) { this.volume = volume; }
  }
}
