package com.investmenttracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

  private String secret;
  private long accessTokenSeconds = 900;
  private long refreshTokenSeconds = 2592000;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getAccessTokenSeconds() {
    return accessTokenSeconds;
  }

  public void setAccessTokenSeconds(long accessTokenSeconds) {
    this.accessTokenSeconds = accessTokenSeconds;
  }

  public long getRefreshTokenSeconds() {
    return refreshTokenSeconds;
  }

  public void setRefreshTokenSeconds(long refreshTokenSeconds) {
    this.refreshTokenSeconds = refreshTokenSeconds;
  }
}
