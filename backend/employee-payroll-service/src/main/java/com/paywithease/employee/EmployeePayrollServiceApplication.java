package com.paywithease.employee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Employee &amp; Payroll Service — employee master and salary structure. Salary runs, payslips, and
 * statutory calculations (PF/ESI/PT/TDS) are a later sprint and are intentionally not implemented
 * here.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class EmployeePayrollServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EmployeePayrollServiceApplication.class, args);
  }
}
