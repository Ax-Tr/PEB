package com.paywithease.purchase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Purchase &amp; Expense Service — records vendor purchase bills (with input GST / ITC computed via
 * the shared GST engine) and business expenses (with maker-checker approval). Emits
 * PURCHASE_BILL_CREATED and EXPENSE_APPROVED for the ledger. All amounts are integer paise.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class PurchaseExpenseServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PurchaseExpenseServiceApplication.class, args);
  }
}
