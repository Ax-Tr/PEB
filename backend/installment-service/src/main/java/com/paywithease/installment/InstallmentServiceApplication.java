package com.paywithease.installment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Installment Service — receivable/payable EMI schedules with due dates, payment tracking, closure,
 * and audited modification. Emits INSTALLMENT_SCHEDULE_CREATED / INSTALLMENT_PAID (consumed by the
 * reminder and analytics services). Cash movement is booked by payment/payout, not here.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class InstallmentServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(InstallmentServiceApplication.class, args);
  }
}
