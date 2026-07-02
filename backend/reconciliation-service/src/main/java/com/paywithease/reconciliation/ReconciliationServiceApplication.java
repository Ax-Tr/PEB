package com.paywithease.reconciliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Reconciliation Service — matches externally-imported bank/settlement rows (the source of truth)
 * against the business's own internal records (payments, invoices, payouts) using a weighted match
 * engine, and surfaces suggestions and exceptions for human review.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class ReconciliationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ReconciliationServiceApplication.class, args);
  }
}
