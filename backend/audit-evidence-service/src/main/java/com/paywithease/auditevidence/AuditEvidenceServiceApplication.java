package com.paywithease.auditevidence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Audit Evidence Service — an immutable, append-only evidence room. Evidence items (uploaded
 * artifacts or system-generated records from domain events) are stored with a SHA-256 content hash
 * so their integrity can be independently re-verified at any time. There is deliberately no update
 * or delete path — evidence survives even the reversal of the transaction it references — and every
 * recorded item emits {@code AUDIT_EVENT_RECORDED}. Auditors can also drive export jobs.
 */
@SpringBootApplication(scanBasePackages = "com.paywithease")
@EntityScan(basePackages = "com.paywithease")
@EnableJpaRepositories(basePackages = "com.paywithease")
public class AuditEvidenceServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuditEvidenceServiceApplication.class, args);
  }
}
