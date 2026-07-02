package com.paywithease.payout.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Append-only maker-checker decision record. */
@Entity
@Table(name = "payout_approvals")
public class PayoutApproval {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "payout_id", length = 26, nullable = false)
  private String payoutId;

  @Column(nullable = false)
  private String decision; // APPROVED, REJECTED

  @Column(name = "approver_id", length = 26, nullable = false)
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
