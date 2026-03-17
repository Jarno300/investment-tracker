package com.investmenttracker;

import com.investmenttracker.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class})
public class InvestmentTrackerApplication {

  public static void main(String[] args) {
    SpringApplication.run(InvestmentTrackerApplication.class, args);
  }
}
