package com.paywithease.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Business Service — consolidates customer, vendor, employee-payroll, product, invoice-gst,
 * purchase-expense, commitment, notification, OCR-document, and CA-collaboration modules.
 *
 * <p>Scans {@code com.paywithease} so common-libraries beans are picked up.
 */
@SpringBootApplication
@ComponentScan(
    basePackages = "com.paywithease",
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern =
              "com\\.paywithease\\.(customer|vendor|employee|product|invoice|purchase|commitment|notification|ocr|cacollaboration)\\.config\\.SecurityConfig"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern =
              "com\\.paywithease\\.(customer|vendor|employee|product|invoice|purchase|commitment|notification|ocr|cacollaboration)\\..*Application")
    })
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class BusinessServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(BusinessServiceApplication.class, args);
  }
}
