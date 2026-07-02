package com.paywithease.cacollaboration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * CA Collaboration Service — a workspace for external accountants / CAs / auditors. It issues
 * role-scoped invitations (revocable mid-review), captures append-only review notes, runs
 * maker-checker report approvals (emitting APPROVAL_REQUESTED / APPROVAL_COMPLETED via the
 * transactional outbox), and maintains the month-end close checklist whose {@code canLockMonth}
 * flag gates the ledger month lock. The AUDITOR collaborator scope is strictly read-only, enforced
 * at the service layer.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class CaCollaborationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(CaCollaborationServiceApplication.class, args);
  }
}
