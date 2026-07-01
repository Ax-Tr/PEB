package com.paywithease.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Tenant/Business Service — business profile, branches, GST/PAN/Udyam, tax profile, and settings.
 * {@code businesses.id} is the canonical tenant_id used across the platform.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class TenantServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(TenantServiceApplication.class, args);
  }
}
