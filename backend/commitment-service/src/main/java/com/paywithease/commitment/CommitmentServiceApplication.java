package com.paywithease.commitment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Commitment Service - records customer/vendor promises with due dates, partial payments, audited
 * rescheduling, and broken-promise visibility.
 */
@SpringBootApplication(scanBasePackages = {"com.paywithease.commitment", "com.paywithease.common"})
public class CommitmentServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(CommitmentServiceApplication.class, args);
  }
}
