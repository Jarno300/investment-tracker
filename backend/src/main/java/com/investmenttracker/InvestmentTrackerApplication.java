package com.investmenttracker;

import com.investmenttracker.config.JwtProperties;
import com.investmenttracker.config.StockApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, StockApiProperties.class})
public class InvestmentTrackerApplication {

  public static void main(String[] args) {
    SpringApplication.run(InvestmentTrackerApplication.class, args);
  }
}
