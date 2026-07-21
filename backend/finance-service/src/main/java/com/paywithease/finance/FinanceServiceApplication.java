package com.paywithease.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Finance Service — consolidates payment-collection, payout, accounting-ledger, installment,
 * transaction-ingestion, reconciliation, compliance-report, analytics, audit-evidence, and
 * ai-automation modules.
 */
@SpringBootApplication
@ComponentScan(
    basePackages = "com.paywithease",
    excludeFilters =
        {
          @ComponentScan.Filter(
              type = FilterType.REGEX,
              pattern =
                  "com\\.paywithease\\.(payment|payout|ledger|installment|ingestion|reconciliation|compliance|analytics|auditevidence|ai)\\.config\\.SecurityConfig"),
          @ComponentScan.Filter(
              type = FilterType.REGEX,
              pattern =
                  "com\\.paywithease\\.(payment|payout|ledger|installment|ingestion|reconciliation|compliance|analytics|auditevidence|ai)\\..*Application")
        })
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class FinanceServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(FinanceServiceApplication.class, args);
  }
}
