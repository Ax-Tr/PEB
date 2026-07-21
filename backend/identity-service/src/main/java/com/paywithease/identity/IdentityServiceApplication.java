package com.paywithease.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Identity &amp; Access Service — OTP login, passkeys, JWT/OIDC, RBAC/ABAC, device binding,
 * sessions. Sprint 0 ships the skeleton (health + ping + baseline schema + outbox/audit wiring);
 * Sprint 1 adds OTP/token/RBAC flows.
 *
 * <p>Scans the shared {@code com.paywithease.common} package so the correlation filter, error
 * handler, outbox/audit writers, and beans are picked up alongside this service's own components.
 */
@SpringBootApplication
@ComponentScan(
    basePackages = "com.paywithease",
    excludeFilters = {
      @ComponentScan.Filter(
          type = org.springframework.context.annotation.FilterType.REGEX,
          pattern = "com\\.paywithease\\.(tenant|privacy)\\.config\\.SecurityConfig"),
      @ComponentScan.Filter(
          type = org.springframework.context.annotation.FilterType.REGEX,
          pattern = "com\\.paywithease\\.(tenant|privacy)\\..*Application")
    })
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class IdentityServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(IdentityServiceApplication.class, args);
  }
}
