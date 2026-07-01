package com.paywithease.vendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Vendor Service — vendor profiles and their bank accounts for payouts. Bank accounts captured from
 * OCR (or entered manually) are saved {@code PENDING_REVIEW} and must be explicitly confirmed by a
 * user before they become usable (product rule #7: OCR results must be user-reviewed before
 * sensitive bank details are usable).
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class VendorServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(VendorServiceApplication.class, args);
  }
}
