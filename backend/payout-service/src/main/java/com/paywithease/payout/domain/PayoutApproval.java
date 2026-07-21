package com.paywithease.payout.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Append-only maker-checker decision record. */
@Entity
@Table(name = "payout_approvals")
public class PayoutApproval {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "payout_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String payoutId;

  @Column(nullable = false)
  private String decision; // APPROVED, REJECTED

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "approver_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String approverId;

  private String reason;

  @Column(name = "decided_at", nullable = false)
  private Instant decidedAt;

  protected PayoutApproval() {}

  public PayoutApproval(
      String id,
      String tenantId,
      String payoutId,
      String decision,
      String approverId,
      String reason,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.payoutId = payoutId;
    this.decision = decision;
    this.approverId = approverId;
    this.reason = reason;
    this.decidedAt = now;
  }

  public String getId() {
    return id;
  }
}
