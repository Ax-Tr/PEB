package com.paywithease.compliance.domain;

/**
 * Compliance report lifecycle. A report is DRAFT when generated, REVIEWED by an accountant/CA,
 * APPROVED (only once the underlying data is reconciled), and FILED only when an official
 * portal/API acknowledgement is recorded — the system never infers "filed".
 */
public enum ComplianceStatus {
  DRAFT,
  REVIEWED,
  APPROVED,
  FILED
}
