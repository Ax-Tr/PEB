package com.paywithease.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway — request routing, authentication enforcement, rate limiting, tenant resolution,
 * correlation IDs, and API versioning. Sprint 0 routes to identity-service and injects correlation
 * IDs; Sprint 1 adds JWT (JWKS) validation and tenant-claim resolution.
 */
@SpringBootApplication
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
