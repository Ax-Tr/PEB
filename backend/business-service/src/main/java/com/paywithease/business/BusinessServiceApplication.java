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
    basePackages = {
      "com.paywithease.business",
      "com.paywithease.common",
      "com.paywithease.employee",
      "com.paywithease.product",
      "com.paywithease.invoice",
      "com.paywithease.purchase",
      "com.paywithease.commitment",
      "com.paywithease.notification",
      "com.paywithease.ocr",
      "com.paywithease.cacollaboration"
    },
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern =
              "com\\.paywithease\\.(employee|product|invoice|purchase|commitment|notification|ocr|cacollaboration)\\.config\\.SecurityConfig"),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern =
              "com\\.paywithease\\.(employee|product|invoice|purchase|commitment|notification|ocr|cacollaboration)\\..*Application")
    })
@EntityScan(
    basePackages = {
      "com.paywithease.business",
      "com.paywithease.common",
      "com.paywithease.employee",
      "com.paywithease.product",
      "com.paywithease.invoice",
      "com.paywithease.purchase",
      "com.paywithease.commitment",
      "com.paywithease.notification",
      "com.paywithease.ocr",
      "com.paywithease.cacollaboration"
    })
@EnableJpaRepositories(
    basePackages = {
      "com.paywithease.business",
      "com.paywithease.common",
      "com.paywithease.employee",
      "com.paywithease.product",
      "com.paywithease.invoice",
      "com.paywithease.purchase",
      "com.paywithease.commitment",
      "com.paywithease.notification",
      "com.paywithease.ocr",
      "com.paywithease.cacollaboration"
    })
public class BusinessServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(BusinessServiceApplication.class, args);
  }
}
