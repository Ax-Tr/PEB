package com.paywithease.invoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Invoice & GST Service — GST-compliant invoices, bills of supply, receipt vouchers, credit/debit
 * notes; document numbering; PDF rendering; and e-invoice / e-way-bill readiness payloads.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class InvoiceGstServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(InvoiceGstServiceApplication.class, args);
  }
}
