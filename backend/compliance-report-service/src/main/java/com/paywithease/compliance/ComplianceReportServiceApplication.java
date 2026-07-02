package com.paywithease.compliance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Compliance Report Service — prepares GST/payroll/TDS/ITR compliance reports from a period
 * read-model built off invoice/purchase/payroll events, and drives the report lifecycle (DRAFT →
 * REVIEWED → APPROVED → FILED) with maker-checker controls. Approval requires reconciled data;
 * FILED requires an official acknowledgement — the service never files with the portal itself.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class ComplianceReportServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ComplianceReportServiceApplication.class, args);
  }
}
