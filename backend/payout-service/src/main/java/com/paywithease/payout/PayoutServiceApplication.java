package com.paywithease.payout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Payout Service — vendor/employee payouts with beneficiary validation, risk-based maker-checker
 * approval, step-up auth, idempotency, and gateway failover. Emits PAYOUT_APPROVAL_* and
 * VENDOR_PAYMENT_* events; the ledger consumer posts the disbursement journal.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class PayoutServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PayoutServiceApplication.class, args);
  }
}
