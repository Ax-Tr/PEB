package com.paywithease.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Payment Collection Service — payment request creation, dynamic UPI QR/link generation, gateway
 * abstraction, signature-verified idempotent webhooks, and settlement status. Emits PAYMENT_*
 * events consumed by invoice/ledger/installment/reconciliation services.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class PaymentCollectionServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentCollectionServiceApplication.class, args);
  }
}
