package com.paywithease.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Accounting Ledger Service — the platform's double-entry general ledger. Seeds a chart of accounts
 * per tenant, posts balanced journals (from the REST API and from upstream invoice/payment events),
 * derives the standard financial statements, and enforces the month-lock lifecycle. Journals are
 * append-only; corrections are reversing entries.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class AccountingLedgerServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AccountingLedgerServiceApplication.class, args);
  }
}
