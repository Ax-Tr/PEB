package com.paywithease.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Transaction Ingestion Service — captures cash/bank/UPI/settlement transactions via manual entry
 * and idempotent statement/feed import, rule-based classification with human review. Imported rows
 * are the reconciliation source of truth; they are not posted to the ledger here.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class IngestionServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(IngestionServiceApplication.class, args);
  }
}
