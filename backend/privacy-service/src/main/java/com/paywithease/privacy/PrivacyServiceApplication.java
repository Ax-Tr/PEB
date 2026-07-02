package com.paywithease.privacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Privacy Service — the DPDP data-principal rights workflow. It intakes access/correction/erasure/
 * portability/grievance requests, verifies the requester's identity before any data is acted on,
 * and produces an honest erasure plan (financial/tax/KYC records are retained under legal hold with
 * linked PII anonymised — never hard-deleted). It emits DSR_RECEIVED / DATA_ERASURE_REQUESTED /
 * DSR_COMPLETED / DPDP_GRIEVANCE_RAISED via the transactional outbox so each owning service can act
 * on its own slice; this service consumes nothing. SLA-tracked and tenant-scoped, with the
 * subject's email encrypted at rest.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class PrivacyServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PrivacyServiceApplication.class, args);
  }
}
