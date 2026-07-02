package com.paywithease.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Analytics Service — an event-fed OLAP read-model powering MSME dashboards: revenue/cost/profit
 * and margins, cashflow, receivables/payables aging, and product profitability, plus a read-model
 * freshness indicator (analytics is eventually consistent). It NEVER queries any OLTP service's
 * database — its only inputs are domain events. The {@link java.time.Clock} bean is provided by
 * common-libraries' CommonBeansConfig via the {@code com.paywithease} component scan.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class AnalyticsServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AnalyticsServiceApplication.class, args);
  }
}
